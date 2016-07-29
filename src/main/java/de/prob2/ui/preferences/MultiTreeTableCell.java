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
		final Class<?> valueType = item.getValueType();
		if (valueType == null) {
			return;
		}
		
		super.startEdit();
		
		if (!this.isEditing() || boolean.class.equals(valueType)) {
			return;
		}
		
		this.setText(null);
		if (int.class.equals(valueType)) {
			final Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.parseInt(item.getValue())));
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
		} else if (String.class.equals(valueType)) {
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
		} else {
			throw new IllegalArgumentException("Unsupported value type: " + valueType);
		}
	}
	
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		if (!boolean.class.equals(this.getTreeTableRow().getItem().getValueType())) {
			this.setGraphic(null);
			this.setText(this.getTreeTableRow().getItem().getValue());
		}
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		if (this.getTreeTableRow() != null && this.getTreeTableRow().getItem() != null) {
			final PrefTreeItem pti = this.getTreeTableRow().getItem();
			if (boolean.class.equals(pti.getValueType())) {
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
