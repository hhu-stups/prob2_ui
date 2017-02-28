package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ConsoleSearchHandler {
	
	protected boolean searchActive = false;
	protected List<SearchResult> searchResults;
	protected int currentSearchIndex = 0;
	public static final String FOUND = "(backward search) '':";
	public static final String NOTFOUND = "(failed backward search) '':"; 
	private final Console parent;
	
	public ConsoleSearchHandler(Console parent) {
		this.searchResults = new ArrayList<>();
		this.parent = parent;
	}
	
	public boolean isActive() {
		return searchActive;
	}
	
	public void activateSearch() {
		currentSearchIndex = 0;
		searchActive = true;
		if(searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public void deactivateSearch() {
		searchResults.clear();
		currentSearchIndex = 0;
		searchActive = false;
	}
	
	protected void searchResult(KeyEvent e) {
		searchResults.clear();
		String key = getSearchCurrent();
		if (e.getCode() == KeyCode.BACK_SPACE) {
			key = key.substring(0, Math.max(0, key.length() - 1));
		} else {
			key += e.getText();
		}
		for(int i = parent.getInstructions().size() - 1; i >= 0; i--) {
			if(parent.getInstructions().get(i).getInstruction().contains(key)) {
				searchResults.add(new SearchResult(parent.getInstructions().get(i).getInstruction(),true));
			}
		}
		if(searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public String getSearchCurrent() {
		int posOfFirstQuotation = parent.getCurrentLine().indexOf(39);
		int posOfLastQuotation = parent.getCurrentLine().lastIndexOf(39);
		return parent.getCurrentLine().substring(posOfFirstQuotation + 1, posOfLastQuotation);
	}
	
	public String getCurrentSearchResult() {
		int posOfColon = parent.getCurrentLine().indexOf(':') + parent.getText().lastIndexOf("\n") + 4;
		return parent.getText().substring(posOfColon, parent.getText().length());
	}
	
	protected void handleKey(KeyEvent e) {
		if(isActive()) {
			currentSearchIndex = 0;
			searchResult(e);
			refreshSearch();
		}
	}
	
	protected void handleEnter() {
		if(isActive()) {
			deactivateSearch();
		}
	}
	
	protected boolean handleDeletion(KeyEvent e) {
		if(isActive()) {
			if(e.getCode() == KeyCode.DELETE) {
				parent.deactivateSearch();
				return true;
			} else if(parent.getCaretPosition() != parent.getLength() - parent.getCurrentLine().length() + parent.getCurrentLine().indexOf("'") + 1) {
				handleKey(e);
			}
		}
		return false;
	}
	
	protected void refreshSearch() {
		String searchPrefix = FOUND;
		if(!searchResults.get(0).getFound()) {
			searchPrefix = NOTFOUND;
		}
		int posOfEnter = parent.getText().lastIndexOf("\n");
		String addition = searchPrefix.substring(0,searchPrefix.length() - 2) + getSearchCurrent() + "':" + searchResults.get(currentSearchIndex).getResult();
		parent.deleteText(posOfEnter + 1, parent.getText().length());
		parent.appendText(addition);
		int posOfColon = parent.getCurrentLine().indexOf(':') + parent.getText().lastIndexOf("\n") + 3;
		parent.moveTo(posOfColon -1);
		parent.setEstimatedScrollY(Double.MAX_VALUE);
	}
	
	protected void searchNext() {
		currentSearchIndex = Math.min(searchResults.size() - 1, currentSearchIndex + 1);
		refreshSearch();
	}

}
