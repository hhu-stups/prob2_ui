package de.prob2.ui.animation.symbolic.testcasegeneration;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class OperationSelectedProperty implements Callback<TableColumn.CellDataFeatures<OperationTableItem, CheckBox>, ObservableValue<CheckBox>> {

	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<OperationTableItem, CheckBox> param) {
		OperationTableItem item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().setValue(item.selected());
		checkBox.selectedProperty().addListener((observable, from, to) -> item.selectedProperty().set(to));
		return new SimpleObjectProperty<>(checkBox);
	}

}
