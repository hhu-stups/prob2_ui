package de.prob2.ui.consoles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;

final class ConsoleHistoryHandler {

    private final Console parent;
    private final ObservableList<String> history;
    private final ObservableList<String> historyView;
    private int historyPosition;
    private String savedInput;

    ConsoleHistoryHandler(Console parent) {
        this.parent = parent;
        this.history = FXCollections.observableArrayList();
        this.historyView = FXCollections.unmodifiableObservableList(this.history);
        this.historyPosition = 0;
        this.savedInput = "";
    }

    public ObservableList<String> getHistory() {
        return this.historyView;
    }

    public void setHistory(Collection<? extends String> history) {
        this.history.setAll(history);
        this.historyPosition = this.history.size();
    }

    public void up() {
        if (this.historyPosition == 0) {
            return;
        }

        if (this.historyPosition == this.history.size()) {
            this.savedInput = this.parent.getInput();
        }
        this.historyPosition--;
        this.parent.setInput(this.history.get(this.historyPosition));
    }

    public void down() {
        if (this.historyPosition == this.history.size()) {
            return;
        }

        this.historyPosition++;
        if (this.historyPosition == this.history.size()) {
            this.parent.setInput(this.savedInput);
        } else {
            this.parent.setInput(this.history.get(this.historyPosition));
        }
    }

    public void enter(String command) {
        this.history.add(command);
        this.historyPosition = this.history.size();
        this.savedInput = "";
    }
}
