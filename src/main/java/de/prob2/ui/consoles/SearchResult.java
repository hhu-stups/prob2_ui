package de.prob2.ui.consoles;

public final class SearchResult {

	private final String result;
	private final boolean found;

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
