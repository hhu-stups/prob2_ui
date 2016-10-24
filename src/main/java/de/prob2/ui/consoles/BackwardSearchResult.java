package de.prob2.ui.consoles;

public class BackwardSearchResult {

	private String result;
	private boolean found;
	
	public BackwardSearchResult(String result, boolean found) {
		this.result = result;
		this.found = found;
	}
	
	public String getResult() {
		return result;
	}
	
	public boolean getFound() {
		return found;
	}
	
}
