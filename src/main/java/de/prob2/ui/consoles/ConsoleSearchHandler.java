package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.List;

import de.prob2.ui.internal.I18n;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class ConsoleSearchHandler {
	
	private boolean searchActive = false;
	private final List<SearchResult> searchResults;
	private int currentSearchIndex = 0;
	private final Console parent;
	private final I18n i18n;
	
	public ConsoleSearchHandler(Console parent, I18n i18n) {
		this.searchResults = new ArrayList<>();
		this.parent = parent;
		this.i18n = i18n;
	}
	
	public boolean isActive() {
		return searchActive;
	}
	
	public void activateSearch() {
		currentSearchIndex = 0;
		searchActive = true;
		if (searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public void deactivateSearch() {
		searchResults.clear();
		currentSearchIndex = 0;
		searchActive = false;
	}
	
	private void searchResult(KeyEvent e) {
		searchResults.clear();
		String key = getSearchCurrent();
		if (e.getCode() == KeyCode.BACK_SPACE) {
			key = key.substring(0, Math.max(0, key.length() - 1));
		} else {
			key += e.getText();
		}
		for (int i = parent.getInstructions().size() - 1; i >= 0; i--) {
			if (parent.getInstructions().get(i).getInstruction().contains(key)) {
				searchResults.add(new SearchResult(parent.getInstructions().get(i).getInstruction(),true));
			}
		}
		if (searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}
	
	public String getSearchCurrent() {
		int posOfFirstQuotation = parent.getLine().indexOf('\'');
		int posOfLastQuotation = parent.getLine().lastIndexOf('\'');
		return parent.getLine().substring(posOfFirstQuotation + 1, posOfLastQuotation);
	}
	
	public String getCurrentSearchResult() {
		return parent.getLine().substring(parent.getLine().indexOf(':') + 1);
	}
	
	void handleKey(KeyEvent e) {
		if (isActive()) {
			currentSearchIndex = 0;
			searchResult(e);
			refreshSearch();
		}
	}
	
	void handleEnter() {
		if (isActive()) {
			deactivateSearch();
		}
	}
	
	boolean handleDeletion(KeyEvent e) {
		if (isActive()) {
			if (e.getCode() == KeyCode.DELETE) {
				parent.deactivateSearch();
				return true;
			} else if (parent.getCaretPosition() != parent.getAbsolutePosition(parent.getLineNumber(), parent.getLine().indexOf('\'')) + 1) {
				handleKey(e);
			}
		}
		return false;
	}
	
	private void refreshSearch() {
		final String addition = i18n.translate(
				searchResults.get(0).getFound() ? "consoles.prompt.backwardSearch" : "consoles.prompt.backwardSearchFailed",
				getSearchCurrent(), searchResults.get(currentSearchIndex).getResult()
		);
		parent.deleteText(parent.getLineStart(), parent.getLength());
		parent.appendText(addition);
		parent.moveTo(parent.getAbsolutePosition(parent.getLineNumber(), parent.getLine().indexOf(':')) - 1);
		parent.scrollYToPixel(Double.MAX_VALUE);
	}
	
	void searchNext() {
		currentSearchIndex = Math.min(searchResults.size() - 1, currentSearchIndex + 1);
		refreshSearch();
	}

}
