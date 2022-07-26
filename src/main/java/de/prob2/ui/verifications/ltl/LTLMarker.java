package de.prob2.ui.verifications.ltl;

public class LTLMarker {
	private final String type;
	private final LTLMark mark;
	private final String msg;

	public LTLMarker(String type, int line, int pos, int length, String msg) {
		this.type = type;
		this.mark = new LTLMark(line, pos, length);
		this.msg = msg;
	}

	public String getType() {
		return type;
	}

	public LTLMark getMark() {
		return mark;
	}

	public String getMsg() {
		return msg;
	}
}
