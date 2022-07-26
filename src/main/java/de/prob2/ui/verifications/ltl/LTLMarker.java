package de.prob2.ui.verifications.ltl;

public class LTLMarker {
	private final String type;
	private final int line;
	private final int pos;
	private final int length;
	private final String msg;

	public LTLMarker(String type, int line, int pos, int length, String msg) {
		this.type = type;
		this.line = line;
		this.pos = pos;
		this.length = length;
		this.msg = msg;
	}

	public String getType() {
		return type;
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

	public String getMsg() {
		return msg;
	}
}
