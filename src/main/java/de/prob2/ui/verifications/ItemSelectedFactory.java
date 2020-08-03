package de.prob2.ui.verifications;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class ItemSelectedFactory implements Callback<TableColumn.CellDataFeatures<IExecutableItem, CheckBox>, ObservableValue<CheckBox>> {
	private final CheckBox selectAll;
	
	public ItemSelectedFactory(final TableView<? extends IExecutableItem> tableView, final CheckBox selectAll) {
		this.selectAll = selectAll;
		
		this.selectAll.setSelected(true);
		this.selectAll.setOnAction(e -> {
			for (IExecutableItem it : tableView.getItems()) {
				it.setSelected(this.selectAll.isSelected());
				tableView.refresh();
			}
		});
	}
	
	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IExecutableItem, CheckBox> param) {
		IExecutableItem item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().setValue(item.selected());
		checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			item.setSelected(newValue);
			this.selectAll.setSelected(param.getTableView().getItems().stream().anyMatch(IExecutableItem::selected));
		});
		return new SimpleObjectProperty<>(checkBox);
	}
}
