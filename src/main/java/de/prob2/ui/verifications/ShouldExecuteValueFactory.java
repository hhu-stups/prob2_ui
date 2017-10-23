package de.prob2.ui.verifications;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ShouldExecuteValueFactory implements Callback<TableColumn.CellDataFeatures<IShouldExecuteItem, CheckBox>, ObservableValue<CheckBox>> {
    @Override
    public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IShouldExecuteItem, CheckBox> param) {
    	IShouldExecuteItem item = param.getValue();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().setValue(item.shouldExecute());
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            item.setShouldExecute(newValue);
        });
        return new SimpleObjectProperty<>(checkBox);
    }
}