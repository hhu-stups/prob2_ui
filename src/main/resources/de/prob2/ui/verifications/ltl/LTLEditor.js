LtlEditor = function() {
	const extern = {};
	extern.cm = null;
	extern.ignorePatternName = null;
	extern.lastParseOk = true;
	extern.parseListeners = [];
	
	/* Create and destroy CodeMirror */
	extern.setCodeMirror = function(codeElement, options = {}) {
		// Remove old CodeMirror instance
		extern.destroy();
		
		if (codeElement != null) {
			// Create new CodeMirror instance
			const cmSettings = {
				lineNumbers: true,
				matchBrackets: true,
				autoCloseBrackets: true,
				extraKeys: {"Ctrl-Space": "autocomplete"},
				gutters: ["CodeMirror-linenumbers", "markers"],
				scrollbarStyle: "simple",
			};
			extern.cm = CodeMirror.fromTextArea(codeElement, cmSettings);
			extern.showPatternMarkers = options.showPatternMarkers;
			extern.mode = options.mode || "parseFormula";
			extern.ignorePatternName = options.ignorePatternName || null;
			
			extern.cm.on("change", function(cm, obj) {
				const marks = extern.cm.getAllMarks();
				marks.forEach(marker => marker.clear());
			});
			
			if (options.parseOnChange) {
				// Enable parse on change
				let delay;
				extern.cm.on("change", function(cm, obj) {
					clearTimeout(delay);
					delay = setTimeout(parseInput, 500);
				});
				parseInput();
			}
			if (options.highlightOperands) {
				// Enable operand highlighting
				extern.cm.on("cursorActivity", function(cm) {
					refreshOperandHighlighting(500);
				});
			}
			if (options.showHints) {
				// Enable autocomplete
				CodeMirror.commands.autocomplete = autocomplete;
			}
		}
	};
	
	extern.destroy = function() {
		if (extern.cm != null) {
			// Remove old CodeMirror instance
			extern.cm.off("change");
			extern.cm.off("cursorActivity");
			CodeMirror.commands.autocomplete = null;
			extern.showPatternMarkers = null;
			extern.lastParseOk = true;
			
			extern.cm.toTextArea();
		}
		extern.cm = null;
		extern.ignorePatternName = null;
		extern.parseListeners = [];
	};
	
	/* Parsing */
	function parseInput() {
		const input = extern.cm.getValue();
		if (extern.mode === "parseFormula") {
			Util.parseFormula(input);
		} else {
			Util.parsePattern(input, extern.ignorePatternName);
		}
	}
	
	extern.parseOk = function(data) {
		clearMarkers();
		if (extern.showPatternMarkers) {
			addMarkers(JSON.parse(data.markers), false);
		}
		addMarkers(JSON.parse(data.warnings));
		
		refreshOperandHighlighting(250);
		
		extern.lastParseOk = true;
		notifyParseListeners();
	};
	
	extern.parseFailed = function(data) {
		clearMarkers();
		addMarkers(JSON.parse(data.errors));
		if (extern.showPatternMarkers) {
			addMarkers(JSON.parse(data.markers), false);
		}
		addMarkers(JSON.parse(data.warnings));
		
		refreshOperandHighlighting(250);
		
		extern.lastParseOk = false;
		notifyParseListeners();
	};
	
	function notifyParseListeners() {
		for (let i = 0; i < extern.parseListeners.length; i++) {
			extern.parseListeners[i](extern.lastParseOk);
		}
	}
	
	/* Operand highlighting */
	function refreshOperandHighlighting(ms) {
		setTimeout(getOperatorAtCursorPosition, ms);
	}
	
	function getOperatorAtCursorPosition() {
		var cursor = extern.cm.getCursor();
		var pos = (cursor.line + 1) + "-" + cursor.ch;
		Util.getOperatorAtPosition(pos);
	}
	
	extern.expressionFound = function(data) {
		removeTextMarkers(["operator", "operand"]);
		highlightOperand(JSON.parse(data.expression));
	};
	
	extern.noExpressionFound = function(data) {
		removeTextMarkers(["operator", "operand"]);
	};
	
	function highlightOperand(expression) {
		setTextMarker(expression.operator, {className: "operator"});
		const operands = expression.operands;
		for (let i = 0; i < operands.length; i++) {
			setTextMarker(operands[i], {className: "operand"});
		}
	}
	
	/* Gutter- and text-markers */
	function addMarkers(markers, textmarker = true) {
		for (let i = 0; i < markers.length; i++) {
			const marker = markers[i];
			const mark = marker.mark;
			const line = mark.line - 1;
			const lineInfo = extern.cm.lineInfo(line);
			// Gutter marker
			const markerElement = makeGutterMarker(lineInfo, marker.type, marker.msg);
			extern.cm.setGutterMarker(line, "markers", markerElement);
			if (marker.type === "pattern" && extern.showPatternMarkers) {
				$(markerElement).click(function() {
					extern.showPatternMarkers(marker.name, mark, marker.stop);
				});
			}
			if (textmarker) {
				// Text marker
				const options = {
					className: marker.type + '-underline',
					title: marker.msg
				};
				setTextMarker(mark, options);
			}
		}
	}
	
	function makeGutterMarker(lineInfo, type, msg) {
		let marker;
		if (typeof lineInfo.gutterMarkers === 'undefined' || lineInfo.gutterMarkers === null) {
			marker = document.createElement("div");
			marker.title = msg;
			marker.className = type;
		} else {
			marker = lineInfo.gutterMarkers['markers'];
			marker.title += '\n' + msg;
		}
		return marker;
	}
	
	function clearMarkers() {
		extern.cm.clearGutter("markers");
		removeTextMarkers(["error-underline", "warning-underline"]);
	}
	
	function setTextMarker(mark, options) {
		const line = mark.line - 1;
		
		const from = {
			line: line,
			ch: mark.pos
		};
		const to = {
			line: line,
			ch: mark.pos + mark.length
		};
		extern.cm.markText(from, to, options);
	}
	
	function removeTextMarkers(classes) {
		const marks = extern.cm.getAllMarks();
		for (let i = 0; i < marks.length; i++) {
			const mark = marks[i];
			if ($.inArray(mark.className, classes) !== -1) {
				mark.clear();
			}
		}
	}
	
	/* Code completion */
	function autocomplete(cm) {
		const WORD = /[\w$]+/;
		const cursor = cm.getCursor();
		const lineInfo = cm.getLine(cursor.line);
		
		// Find characters before cursor that belong to the current word
		let start = cursor.ch;
		while (start && WORD.test(lineInfo.charAt(start - 1))) {
			--start;
		}
		let startsWith = "";
		if (start !== cursor.ch) {
			startsWith = lineInfo.slice(start, cursor.ch);
		}
		
		Util.getAutoCompleteList(cursor.line, cursor.ch, startsWith, cm.getValue());
	}
	
	extern.showHint = function(data) {
		const options = {
			hints: JSON.parse(data.hints)
		};
		CodeMirror.showHint(extern.cm, CodeMirror.hint.ltl, options);
	};
	
	return extern;
}();
