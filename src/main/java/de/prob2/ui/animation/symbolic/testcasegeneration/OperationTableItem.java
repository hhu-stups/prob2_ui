package de.prob2.ui.animation.symbolic.testcasegeneration;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class OperationTableItem {

    private String operation;

    private BooleanProperty selected;

    public OperationTableItem(String operation, boolean selected) {
        this.operation = operation;
        this.selected = new SimpleBooleanProperty(selected);
    }

    public String getOperation() {
        return operation;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean selected() {
        return selected.get();
    }
}
