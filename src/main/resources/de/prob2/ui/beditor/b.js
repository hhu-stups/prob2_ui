CodeMirror.defineMode("b", function() {
	return {
		token: function(stream) {
			t = blexer.getNextToken();
			if (stream.match(t.getText(), true)) {
				return blexer.getStyleclassFromToken(t);
			}
			blexer.jslog(t.getClass().toString());
			blexer.jslog(t.getText());
			return 'b-nothing';
		}
	};
});

CodeMirror.defineMIME("text/b", "b");
