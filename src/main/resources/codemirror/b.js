CodeMirror.defineMode("b", function() {
	return {
		startState : function(basecolumn) {
			return {
				"comment" : false
			};
		},
		token: function(stream, state) {			
			if (stream.match(/\/\*/, true)) {
				if(blexer.size() > 0) {
					blexer.poll();
				}
				state.comment = true;
				return 'b-comment';
			}

			if (stream.match(/\*\//, true)) {
				state.comment = false;
				return 'b-comment';
			}

			if (state.comment) {
				stream.next();
				return 'b-comment';
			} else if(blexer !== null) {
				if(blexer.size() > 0) {
					var t = blexer.peek();
					if(stream.match(t.getText(), true)) {
						blexer.poll();	
						return blexer.getStyleClassFromToken(t);
					} else {
						if (blexer.firstLineSize() > 0) {
							t = blexer.firstLinePeek();
							if(stream.match(t.getText(), true)) {
								blexer.firstLinePoll();
								return blexer.getStyleClassFromToken(t);
							}
						} 
					}
				}
				stream.next();
				return "b-nothing";
			}
		}
	};
});

CodeMirror.defineMIME("text/b", "b");



