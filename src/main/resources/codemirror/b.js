CodeMirror.defineMode("b", function() {
	return {
		startState : function(basecolumn) {
			return {
				"comment" : false
			};
		},
		token: function(stream, state) {
			if (stream.match(/\/\*/, true)) {
				blexer.poll();
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
			} else {
				var t = blexer.peek();

				if(t !== null) {
					if(stream.match(t.getText(), true)) {
						blexer.poll();	
						return blexer.getStyleClassFromToken(t);
					} else {
						t = blexer.firstLinePeek();
						if (t !== null && stream.match(t.getText(), true)) {
							blexer.firstLinePoll();
							return blexer.getStyleClassFromToken(t);
						}
					}
				}
				stream.next();
			}
		}
	};
});

CodeMirror.defineMIME("text/b", "b");



