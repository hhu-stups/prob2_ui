CodeMirror.defineMode("b", function() {
	return {
		startState : function(basecolumn) {
			return {
				"comment" : false
			};
		},
		token: function(stream, state) {
			if (stream.match(/\/\*/, true)) {
				blexer.getNextToken();
				state.comment = true;
				return 'b-comment';
			}

			if (stream.match(/\*\//, true)) {
				state.comment = false;
				return 'b-comment';
			}
			
			if (state.comment == true) {
				stream.next();
				return 'b-comment';
			} else {
				var t = blexer.getNextToken();
				if (t !== null && stream.match(t.getText(), true)) {
					return blexer.getStyleClassFromToken(t);
				}
				stream.next();
				return 'b-nothing';
			}
		}
	};
});

CodeMirror.defineMIME("text/b", "b");



