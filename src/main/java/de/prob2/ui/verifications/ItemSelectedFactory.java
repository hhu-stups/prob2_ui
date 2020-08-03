package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ItemSelectedFactory implements Callback<TableColumn.CellDataFeatures<IExecutableItem, CheckBox>, ObservableValue<CheckBox>> {
	private final BooleanProperty selectAllProperty;
	
	public ItemSelectedFactory(final BooleanProperty selectAllProperty) {
		this.selectAllProperty = selectAllProperty;
	}
	
	public ItemSelectedFactory(final CheckBox selectAll) {
		this(selectAll.selectedProperty());
	}
	
	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IExecutableItem, CheckBox> param) {
		IExecutableItem item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().setValue(item.selected());
		checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			item.setSelected(newValue);
			this.selectAllProperty.set(param.getTableView().getItems().stream().anyMatch(IExecutableItem::selected));
		});
		return new SimpleObjectProperty<>(checkBox);
	}
}
