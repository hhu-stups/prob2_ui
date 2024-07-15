package de.prob2.ui.verifications;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class ItemSelectedFactory<T extends ISelectableTask> implements Callback<TableColumn.CellDataFeatures<T, CheckBox>, ObservableValue<CheckBox>> {
	private final CheckBox selectAll;
	
	public ItemSelectedFactory(final TableView<T> tableView, final CheckBox selectAll) {
		this.selectAll = selectAll;
		
		this.selectAll.setSelected(true);
		// selectAll can be (de)selected manually by the user or automatically when one of the individual checkboxes is (de)selected.
		// We want to update the individual checkboxes only if selectAll was (de)selected by the user, so we use the onAction callback instead of a listener on the selected property.
		this.selectAll.setOnAction(e -> {
			// Changing an item's selected state will automatically (de)select selectAll,
			// which can overwrite the selection change from the user.
			// So we need to remember the selection state immediately after the user has changed it and before it is automatically overwritten in the loop.
			// Once the loop has finished, all items are either selected or deselected,
			// and the selectAll state will automatically update back to what the user has selected.
			final boolean selected = this.selectAll.isSelected();
			for (ISelectableTask it : tableView.getItems()) {
				it.setSelected(selected);
			}
		});
	}
	
	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<T, CheckBox> param) {
		ISelectableTask item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().bindBidirectional(item.selectedProperty());
		item.selectedProperty().addListener(o ->
			this.selectAll.setSelected(param.getTableView().getItems().stream().anyMatch(ISelectableTask::selected))
		);
		return new SimpleObjectProperty<>(checkBox);
	}
}
