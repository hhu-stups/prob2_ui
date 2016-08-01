package de.prob2.ui.preferences;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;

public class MultiTreeTableCell<S extends PrefTreeItem> extends TreeTableCell<S, String> {
	public MultiTreeTableCell() {
		super();
	}
	
	@Override
	public void startEdit() {
		final PrefTreeItem item = this.getTreeTableRow().getItem();
		final PreferenceType valueType = item.getValueType();
		if (valueType == null) {
			return;
		}
		
		super.startEdit();
		
		if (!this.isEditing() || "bool".equals(valueType.getType())) {
			return;
		}
		
		this.setText(null);
		switch (valueType.getType()) {
			case "int":
			case "nat":
			case "nat1":
			case "neg":
				final Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
					"nat1".equals(valueType.getType()) ? 1 : "nat".equals(valueType.getType()) ? 0 : Integer.MIN_VALUE, // min: 1 for nat1, 0 for nat, MIN_VALUE otherwise
					"neg".equals(valueType.getType()) ? 0 : Integer.MAX_VALUE, // max: 0 for neg, MAX_VALUE otherwise
					Integer.parseInt(item.getValue())
				));
				spinner.setEditable(true);
				spinner.getEditor().setOnAction(event -> {
					this.commitEdit(spinner.getEditor().getText());
				});
				spinner.getEditor().setOnKeyReleased(event -> {
					if (event.getCode() == KeyCode.ESCAPE) {
						this.cancelEdit();
					}
				});
				this.setGraphic(spinner);
				spinner.getEditor().requestFocus();
				spinner.getEditor().selectAll();
				break;
			
			case "path":
				// TODO
			
			case "[]":
				// TODO
			
			case "string":
			default:
				final TextField textField = new TextField(item.getValue());
				textField.setOnAction(event -> {
					this.commitEdit(textField.getText());
				});
				textField.setOnKeyReleased(event -> {
					if (event.getCode() == KeyCode.ESCAPE) {
						this.cancelEdit();
					}
				});
				this.setGraphic(textField);
				textField.requestFocus();
				textField.selectAll();
		}
	}
	
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		if (!"bool".equals(this.getTreeTableRow().getItem().getValueType().getType())) {
			this.setGraphic(null);
			this.setText(this.getTreeTableRow().getItem().getValue());
		}
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		if (this.getTreeTableRow() != null && this.getTreeTableRow().getItem() != null) {
			final PrefTreeItem pti = this.getTreeTableRow().getItem();
			if (pti.getValueType() != null && "bool".equals(pti.getValueType().getType())) {
				if (this.getGraphic() instanceof CheckBox) {
					((CheckBox)this.getGraphic()).setSelected("true".equals(pti.getValue()));
				} else {
					final CheckBox checkBox = new CheckBox();
					checkBox.setSelected("true".equals(pti.getValue()));
					checkBox.setOnAction(event -> {
						final boolean selected = checkBox.isSelected();
						this.getTreeTableView().edit(this.getTreeTableRow().getIndex(), this.getTableColumn());
						this.commitEdit("" + selected);
					});
					this.setText(null);
					this.setGraphic(checkBox);
				}
			} else {
				this.setGraphic(null);
				this.setText(this.getTreeTableRow().getItem().getValue());
			}
		} else {
			this.setGraphic(null);
			this.setText(item);
		}
	}
}
