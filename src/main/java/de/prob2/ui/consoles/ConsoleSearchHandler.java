package de.prob2.ui.consoles;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;

final class ConsoleSearchHandler {

    private final Console parent;
    private final BooleanProperty searchActive;
    private final ObservableList<String> searchResults;
    private final IntegerProperty currentSearchIndex;
    private final StringBinding currentSearchResult;

    ConsoleSearchHandler(Console parent) {
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
        this.searchActive.addListener((o, from, to) -> this.update());
        this.parent.inputProperty().addListener((o, from, to) -> this.update());
    }

    public BooleanProperty searchActiveProperty() {
        return searchActive;
    }

    public boolean isActive() {
        return searchActive.get();
    }

    public void activateSearch() {
        this.searchActive.set(true);
    }

    public void deactivateSearch() {
        searchActive.set(false);
    }

    public StringBinding currentSearchResultProperty() {
        return currentSearchResult;
    }

    private void update() {
        currentSearchIndex.set(0);
        searchResults.clear();

        if (isActive()) {
            String key = parent.getInput();
            for (int i = parent.getHistory().size() - 1; i >= 0; i--) {
                String insn = parent.getHistory().get(i);
                if (insn.contains(key)) {
                    searchResults.add(insn);
                }
            }
        }
    }

    public void handleEnter() {
        if (isActive()) {
            deactivateSearch();
        }
    }

    public boolean handleDeletion(KeyEvent e) {
		/*if (isActive()) {
			if (e.getCode() == KeyCode.DELETE) {
				parent.deactivateSearch();
				return true;
			} else if (parent.getCaretPosition() != parent.getAbsolutePosition(parent.getLineNumber(), parent.getLine().indexOf('\'')) + 1) {
				update();
			}
		}*/

        return false;
    }

    public void searchNext() {
        currentSearchIndex.set(Math.min(searchResults.size() - 1, currentSearchIndex.get() + 1));
    }
}
