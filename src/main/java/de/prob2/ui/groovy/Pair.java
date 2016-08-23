package de.prob2.ui.groovy;

public class Pair {
	
	private String console;
	private String result;
	
	public Pair(String console, String result) {
		this.console = console;
		this.result = result;
	}
	
	public String toString() {
		return console + result;
	}

}
