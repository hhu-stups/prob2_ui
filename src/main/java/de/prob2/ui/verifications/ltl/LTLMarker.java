package de.prob2.ui.verifications.ltl;

import org.antlr.v4.runtime.Token;

public class LTLMarker {

	private String type;
	private LTLMark mark;
	private String msg;
	private String name;
	private LTLMark stop;

	public LTLMarker(String type, int line, int pos, int length, String msg) {
		this.type = type;
		this.mark = new LTLMark(line, pos, length);
		this.msg = msg;
	}

	public LTLMarker(String type, Token token, int length, String msg) {
		this.type = type;
		this.mark = new LTLMark(token, length);
		this.msg = msg;
	}

	public LTLMarker(String type, Token start, Token stop, String name, String msg) {
		this.type = type;
		this.mark = new LTLMark(start, 1);
		this.msg = msg;
		this.stop = new LTLMark(stop, stop.getStopIndex() - stop.getStartIndex() + 1);
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LTLMark getMark() {
		return mark;
	}

	public void setMark(LTLMark mark) {
		this.mark = mark;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public LTLMark getStop() {
		return stop;
	}

	public void setStop(LTLMark stop) {
		this.stop = stop;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}