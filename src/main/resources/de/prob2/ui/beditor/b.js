CodeMirror.defineMode("b", function() {
	return {
        startState : function(basecolumn) {
            return {
                "comment" : false
            };
        },
		token: function(stream, state) {
            if (stream.match(/\/\*/, true)) {
                t = blexer.getNextToken();
                state.comment = true
                return 'b-comment'
            }

            if (stream.match(/\*\//, true)) {
                state.comment = false
                return 'b-comment'
            }
            if(state.comment == false) {

                t = blexer.getNextToken();
                if (stream.match(t.getText(), true)) {
                    return blexer.getStyleclassFromToken(t);
                }

            } else {
                stream.next();
                return 'b-comment';
            }
            stream.next();
		}
	};
});

CodeMirror.defineMIME("text/b", "b");
