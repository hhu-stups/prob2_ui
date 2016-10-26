package de.prob2.ui.consoles;

public class SearchResult {

	private String result;
	private boolean found;
	
	public SearchResult(String result, boolean found) {
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
