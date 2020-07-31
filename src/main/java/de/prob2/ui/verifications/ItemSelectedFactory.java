package de.prob2.ui.verifications;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ItemSelectedFactory implements Callback<TableColumn.CellDataFeatures<IExecutableItem, CheckBox>, ObservableValue<CheckBox>> {
	private final ISelectableCheckingView view;
	
	public ItemSelectedFactory(final ISelectableCheckingView view) {
		this.view = view;
	}
	
	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IExecutableItem, CheckBox> param) {
		IExecutableItem item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().setValue(item.selected());
		checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			item.setSelected(newValue);
			view.updateSelectViews();
		});
		return new SimpleObjectProperty<>(checkBox);
	}
}
