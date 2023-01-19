package de.prob2.ui.consoles;

import de.prob2.ui.internal.I18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class ConsoleSearchHandler {

	private final I18n i18n;
	private final Console parent;
	private final BooleanProperty searchActive;
	private final ObservableList<String> searchResults;
	private final IntegerProperty currentSearchIndex;
	private final StringBinding currentSearchResult;

	public ConsoleSearchHandler(I18n i18n, Console parent) {
		this.i18n = i18n;
		this.parent = parent;
		this.searchActive = new SimpleBooleanProperty(false);
		this.searchResults = FXCollections.observableArrayList();
		this.currentSearchIndex = new SimpleIntegerProperty(0);
		this.currentSearchResult = Bindings.createStringBinding(() -> {
			int idx = this.currentSearchIndex.get();
			if (this.searchActive.get() && 0 <= idx && idx < this.searchResults.size()) {
				return this.searchResults.get(idx);
			} else {
				return null;
			}
		}, this.searchActive, this.searchResults, this.currentSearchIndex);
	}

	public BooleanProperty searchActiveProperty() {
		return searchActive;
	}

	public boolean isActive() {
		return searchActive.get();
	}

	public void activateSearch() {
		this.searchActive.set(true);
		reset();
	}

	public void deactivateSearch() {
		searchActive.set(false);
		reset();
	}

	private void reset() {
		this.searchResults.clear();
		this.currentSearchIndex.set(0);
	}

	public StringBinding currentSearchResultProperty() {
		return currentSearchResult;
	}

	private void searchResult() {
		searchResults.clear();
		String key = getSearchCurrent();
		// System.out.println("searching for '" + key + "'");
		for (int i = parent.getHistory().size() - 1; i >= 0; i--) {
			if (parent.getHistory().get(i).getInstruction().contains(key)) {
				searchResults.add(new SearchResult(parent.getHistory().get(i).getInstruction(), true));
			}
		}
		if (searchResults.isEmpty()) {
			searchResults.add(new SearchResult(getCurrentSearchResult(), false));
		}
	}

	void update() {
		if (isActive()) {
			currentSearchIndex = 0;
			searchResult();
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
				update();
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

	public void searchNext() {
		currentSearchIndex = Math.min(searchResults.size() - 1, currentSearchIndex + 1);
		refreshSearch();
	}
}
