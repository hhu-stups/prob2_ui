package de.prob2.ui.verifications.ltl.patterns;

import org.antlr.v4.runtime.Token;

public class LTLPatternMarker {

	private String type;
	private LTLPatternMark mark;
	private String msg;
	private String name;
	private LTLPatternMark stop;

	public LTLPatternMarker(String type, int line, int pos, int length, String msg) {
		this.type = type;
		this.mark = new LTLPatternMark(line, pos, length);
		this.msg = msg;
	}

	public LTLPatternMarker(String type, Token token, int length, String msg) {
		this.type = type;
		this.mark = new LTLPatternMark(token, length);
		this.msg = msg;
	}

	public LTLPatternMarker(String type, Token start, Token stop, String name, String msg) {
		this.type = type;
		this.mark = new LTLPatternMark(start, 1);
		this.msg = msg;
		this.stop = new LTLPatternMark(stop, stop.getStopIndex() - stop.getStartIndex() + 1);
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LTLPatternMark getMark() {
		return mark;
	}

	public void setMark(LTLPatternMark mark) {
		this.mark = mark;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public LTLPatternMark getStop() {
		return stop;
	}

	public void setStop(LTLPatternMark stop) {
		this.stop = stop;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}