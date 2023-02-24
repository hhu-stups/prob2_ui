package de.prob2.ui.consoles;

import java.util.Collection;
import java.util.stream.Collectors;

import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

final class ConsoleHistoryAndSearchHandler {

	private final Console parent;
	private final BooleanProperty searchActive;
	private final BooleanProperty searchFailed;
	private final StringProperty currentSearchResult;
	private final ObservableList<String> history;
	private final ObservableList<String> historyView;
	private int historyPosition;
	private String savedInput;

	ConsoleHistoryAndSearchHandler(Console parent) {
		this.parent = parent;
		this.searchActive = new SimpleBooleanProperty(false);
		this.searchFailed = new SimpleBooleanProperty(false);
		this.currentSearchResult = new SimpleStringProperty();
		this.history = FXCollections.observableArrayList();
		this.historyView = FXCollections.unmodifiableObservableList(this.history);
		this.historyPosition = 0;
		this.savedInput = "";

		this.searchActive.addListener((o, from, to) -> {
			if (to) {
				this.searchFailed.set(false);
				this.savedInput = this.parent.getInput();
				this.parent.setInput("");
			} else {
				this.searchFailed.set(false);
				this.parent.setInput(this.getCurrentSearchResult());
				this.savedInput = this.parent.getInput();
			}

			this.updateSearch();
		});
		this.parent.inputProperty().addListener((o, from, to) -> this.updateSearch());
	}

	private void updateSearch() {
		if (!this.isSearchActive()) {
			this.currentSearchResult.set(null);
			return;
		}

		String searchText = this.parent.getInput();
		boolean failed = true;
		for (int i = this.historyPosition; i >= 0; i--) {
			String cmd = i == this.history.size() ? this.savedInput : this.history.get(i);
			if (cmd.contains(searchText)) {
				this.historyPosition = i;
				this.currentSearchResult.set(cmd);
				failed = false;
				break;
			}
		}

		this.searchFailed.set(failed);
	}

	public BooleanProperty searchActiveProperty() {
		return this.searchActive;
	}

	public boolean isSearchActive() {
		return this.searchActiveProperty().get();
	}

	public void setSearchActive(boolean active) {
		this.searchActiveProperty().set(active);
	}

	public ObservableBooleanValue searchFailedProperty() {
		return this.searchFailed;
	}

	public boolean isSearchFailed() {
		return this.searchFailedProperty().get();
	}

	public ReadOnlyStringProperty currentSearchResultProperty() {
		return this.currentSearchResult;
	}

	public String getCurrentSearchResult() {
		String currentSearchResult = this.currentSearchResult.get();
		return currentSearchResult != null ? currentSearchResult : "";
	}

	public ObservableList<String> getHistory() {
		return this.historyView;
	}

	public void setHistory(Collection<? extends String> history) {
		this.history.setAll(history.stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList()));
		this.historyPosition = this.history.size();
	}

	public void up() {
		if (this.historyPosition <= 0) {
			this.historyPosition = 0;
			return;
		}

		if (this.historyPosition >= this.history.size()) {
			this.historyPosition = this.history.size();
			this.savedInput = this.parent.getInput();
		}
		this.historyPosition--;
		this.parent.setInput(this.history.get(this.historyPosition));
	}

	public void down() {
		if (this.historyPosition >= this.history.size()) {
			this.historyPosition = this.history.size();
			return;
		}

		if (this.historyPosition < 0) {
			this.historyPosition = 0;
		}
		this.historyPosition++;
		if (this.historyPosition == this.history.size()) {
			this.parent.setInput(this.savedInput);
		} else {
			this.parent.setInput(this.history.get(this.historyPosition));
		}
	}

	public void enter(String command) {
		if (command != null && !command.isEmpty()) {
			this.history.add(command);
		}

		this.historyPosition = this.history.size();
		this.savedInput = "";
	}

	public void searchNext() {
		if (!isSearchActive()) {
			return;
		}

		if (this.historyPosition > 0) {
			this.historyPosition--;
			this.updateSearch();
		} else {
			this.searchFailed.set(true);
		}
	}
}
