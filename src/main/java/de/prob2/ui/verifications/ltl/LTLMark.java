package de.prob2.ui.verifications.ltl;

public class LTLMark {
	private final int line;
	private final int pos;
	private final int length;

	public LTLMark(int line, int pos, int length) {
		this.line = line;
		this.pos = pos;
		this.length = length;
	}

	public int getLine() {
		return line;
	}

	public int getPos() {
		return pos;
	}

	public int getLength() {
		return length;
	}
}
