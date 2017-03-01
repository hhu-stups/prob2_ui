CodeMirror.defineMode("b", function() {
	return {
		token: function(stream) {
			stream.next();
			blexer.jslog(blexer.getLength());
			return blexer.getNextToken();
		}
	};
});

CodeMirror.defineMIME("text/b", "b");
