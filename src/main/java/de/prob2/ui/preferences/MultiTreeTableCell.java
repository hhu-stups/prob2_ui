package de.prob2.ui.preferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.input.KeyCode;

public class MultiTreeTableCell<S extends PrefTreeItem> extends TreeTableCell<S, String> {
	// Value types that are always editable, such as bool (CheckBox) or [] (ComboBox).
	private static final Set<String> ALWAYS_EDITABLE;
	
	static {
		final Set<String> alwaysEditable = new HashSet<>();
		alwaysEditable.add("bool");
		alwaysEditable.add("[]");
		ALWAYS_EDITABLE = Collections.unmodifiableSet(alwaysEditable);
	}
	
	public MultiTreeTableCell() {
		super();
	}
	
	// Edit this cell and commit an edit with newValue right away.
	// This is necessary to update cell values in a way that triggers onEditCommit.
	private void instantEdit(final String newValue) {
		// Store the cell currently being edited, in case we recurse (yes, this can actually happen).
		final TreeTablePosition<S, ?> lastEditingCell = this.getTreeTableView().getEditingCell();
		
		this.getTreeTableView().edit(this.getTreeTableRow().getIndex(), this.getTableColumn());
		this.commitEdit(newValue);
		
		// Restore the last editing cell, if necessary.
		if (lastEditingCell != null) {
			this.getTreeTableView().edit(lastEditingCell.getRow(), lastEditingCell.getTableColumn());
		}
	}
	
	@Override
	public void startEdit() {
		final PrefTreeItem item = this.getTreeTableRow().getItem();
		final PreferenceType valueType = item.getValueType();
		// If there is no valueType, this is a category row, which can't be edited.
		if (valueType == null) {
			return;
		}
		
		super.startEdit();
		
		// If the super method decided that we actually shouldn't be editing, don't.
		// For "always editable" cells nothing else should be done, so return for those too.
		if (!this.isEditing() || ALWAYS_EDITABLE.contains(valueType.getType())) {
			return;
		}
		
		this.setText(null);
		switch (valueType.getType()) {
			case "int":
			case "nat":
			case "nat1":
			case "neg":
				// Integer types get a spinner.
				final Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
					"nat1".equals(valueType.getType()) ? 1 : "nat".equals(valueType.getType()) ? 0 : Integer.MIN_VALUE, // Minimum: 1 for nat1, 0 for nat, MIN_VALUE otherwise.
					"neg".equals(valueType.getType()) ? 0 : Integer.MAX_VALUE, // Maximum: 0 for neg, MAX_VALUE otherwise.
					Integer.parseInt(item.getValue()) // Current value.
				));
				spinner.setEditable(true);
				// Commit by pressing Enter in the spinner editor.
				spinner.getEditor().setOnAction(event -> {
					this.commitEdit(spinner.getEditor().getText());
				});
				// Cancel by pressing Escape in the spinner editor.
				spinner.getEditor().setOnKeyReleased(event -> {
					if (event.getCode() == KeyCode.ESCAPE) {
						this.cancelEdit();
					}
				});
				this.setGraphic(spinner);
				// Request focus on the spinner editor so the user can start typing right away.
				spinner.getEditor().requestFocus();
				spinner.getEditor().selectAll();
				break;
			
			case "path":
				// TODO
			
			case "string":
			default:
				// Default case for strings and unknown types, simply display a text field.
				final TextField textField = new TextField(item.getValue());
				// Commit by pressing Enter in the text field.
				textField.setOnAction(event -> {
					this.commitEdit(textField.getText());
				});
				// Cancel by pressing Escape in the text field.
				textField.setOnKeyReleased(event -> {
					if (event.getCode() == KeyCode.ESCAPE) {
						this.cancelEdit();
					}
				});
				this.setGraphic(textField);
				// Request focus on the text field so the user can start typing right away.
				textField.requestFocus();
				textField.selectAll();
		}
	}
	
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		// Revert to normal text, unless this is an "always editable" cell.
		if (!ALWAYS_EDITABLE.contains(this.getTreeTableRow().getItem().getValueType().getType())) {
			this.setGraphic(null);
			this.setText(this.getTreeTableRow().getItem().getValue());
		}
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		if (this.getTreeTableRow() != null && this.getTreeTableRow().getItem() != null) {
			// Item is available, which means we can do fancy stuff!
			final PrefTreeItem pti = this.getTreeTableRow().getItem();
			if (pti.getValueType() == null) {
				// If there is no value type (for categories for example), just display the value text.
				this.setGraphic(null);
				this.setText(this.getTreeTableRow().getItem().getValue());
			} else {
				// Handle the special cases for "always editable" cells.
				switch (pti.getValueType().getType()) {
					case "bool":
						// Booleans get a CheckBox.
						if (this.getGraphic() instanceof CheckBox) {
							// CheckBox already exists, so we only need to update the value.
							((CheckBox)this.getGraphic()).setSelected("true".equals(pti.getValue()));
						} else {
							// CheckBox doesn't exist yet, so create it.
							final CheckBox checkBox = new CheckBox();
							checkBox.setSelected("true".equals(pti.getValue()));
							checkBox.setOnAction(event -> {
								this.instantEdit("" + checkBox.isSelected());
							});
							this.setText(null);
							this.setGraphic(checkBox);
						}
						break;
					
					case "[]":
						// Lists get a ComboBox.
						if (this.getGraphic() instanceof ComboBox) {
							// ComboBox already exists, so we only need to update the selection.
							@SuppressWarnings("unchecked")
							final ComboBox<String> comboBox = (ComboBox<String>)this.getGraphic();
							comboBox.getSelectionModel().select(pti.getValue());
						} else {
							// ComboBox doesn't exist yet, so create it.
							final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(pti.getValueType().getValues()));
							comboBox.getSelectionModel().select(pti.getValue());
							comboBox.setOnAction(event -> {
								this.instantEdit(comboBox.getValue());
							});
							this.setText(null);
							this.setGraphic(comboBox);
						}
						break;
					
					default:
						// Cell is not always editable, so display normal text when not eiditn
						this.setGraphic(null);
						this.setText(this.getTreeTableRow().getItem().getValue());
				}
			}
		} else {
			// Row or item is null, so display the item text as is.
			this.setGraphic(null);
			this.setText(item);
		}
	}
}
