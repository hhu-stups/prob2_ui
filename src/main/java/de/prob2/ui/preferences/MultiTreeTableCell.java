package de.prob2.ui.preferences;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class MultiTreeTableCell<S extends PrefTreeItem> extends TreeTableCell<S, String> {
	// Valid values for named list-like types.
	private static final Map<String, String[]> VALID_TYPE_VALUES;
	
	private Logger logger = LoggerFactory.getLogger(MultiTreeTableCell.class);

	
	static {
		final Map<String, String[]> validTypeValues = new HashMap<>();
		validTypeValues.put("dot_line_style", new String[] {
			"solid",
			"dashed",
			"dotted",
			"bold",
			"invis",
		});
		validTypeValues.put("dot_shape", new String[] {
			"triangle",
			"ellipse",
			"box",
			"diamond",
			"hexagon",
			"octagon",
			"house",
			"invtriangle",
			"invhouse",
			"invtrapez",
			"doubleoctagon",
			"egg",
		});
		validTypeValues.put("por", new String[] {
			"off",
			"ample_sets",
			"sleep_sets",
		});
		validTypeValues.put("text_encoding", new String[] {
			"auto",
			"ISO-8859-1",
			"ISO-8859-2",
			"ISO-8859-15",
			"UTF-8",
			"UTF-16",
			"UTF-16LE",
			"UTF-16BE",
			"UTF-32",
			"UTF-32LE",
			"UTF-32BE",
			"ANSI_X3.4-1968",
			"windows 1252",
		});
		VALID_TYPE_VALUES = Collections.unmodifiableMap(validTypeValues);
	}
	
	// Value types that are always editable, such as bool (CheckBox) or [] (ComboBox).
	private static final Set<String> ALWAYS_EDITABLE;
	
	static {
		final Set<String> alwaysEditable = new HashSet<>();
		alwaysEditable.add("bool");
		alwaysEditable.add("rgb_color");
		alwaysEditable.add("[]");
		alwaysEditable.addAll(VALID_TYPE_VALUES.keySet());
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
		final ProBPreferenceType valueType = item.getValueType();
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
				spinner.getEditor().setOnAction(event -> this.commitEdit(spinner.getEditor().getText()));
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
			case "file_path":
				final FileChooser fileChooser = new FileChooser();
				final File initialFileDirectory = new File(item.getValue()).getParentFile();
				if (initialFileDirectory.exists()) {
					fileChooser.setInitialDirectory(initialFileDirectory);
				}
				final File chosenFile = fileChooser.showOpenDialog(this.getScene().getWindow());
				if (chosenFile == null) {
					this.cancelEdit();
				} else {
					this.commitEdit(chosenFile.getPath());
				}
				break;
			
			case "directory_path":
				final DirectoryChooser directoryChooser = new DirectoryChooser();
				final File initialDirectory = new File(item.getValue());
				if (initialDirectory.exists()) {
					directoryChooser.setInitialDirectory(initialDirectory);
				}
				final File chosenDirectory = directoryChooser.showDialog(this.getScene().getWindow());
				if (chosenDirectory == null) {
					this.cancelEdit();
				} else {
					this.commitEdit(chosenDirectory.getPath());
				}
				break;
			
			case "string":
			default:
				// Default case for strings and unknown types, simply display a text field.
				final TextField textField = new TextField(item.getValue());
				// Commit by pressing Enter in the text field.
				textField.setOnAction(event -> this.commitEdit(textField.getText()));
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
				final String type = pti.getValueType().getType();
				if ("bool".equals(type)) {
					// Booleans get a CheckBox.
					if (this.getGraphic() instanceof CheckBox) {
						// CheckBox already exists, so we only need to update the value.
						((CheckBox)this.getGraphic()).setSelected("true".equals(pti.getValue()));
					} else {
						// CheckBox doesn't exist yet, so create it.
						final CheckBox checkBox = new CheckBox();
						checkBox.setSelected("true".equals(pti.getValue()));
						checkBox.setOnAction(event -> this.instantEdit("" + checkBox.isSelected()));
						this.setText(null);
						this.setGraphic(checkBox);
					}
				} else if ("rgb_color".equals(type)) {
					// Colors get a ColorPicker.
					Color color;
					try {
						color = Color.web(pti.getValue());
					} catch (final IllegalArgumentException exc) {
						logger.error("Invalid color",exc);
						color = Color.color(1.0, 0.0, 1.0);
					}
					if (this.getGraphic() instanceof ColorPicker) {
						final ColorPicker colorPicker = (ColorPicker)this.getGraphic();
						colorPicker.setValue(color);
					} else {
						final ColorPicker colorPicker = new ColorPicker(color);
						colorPicker.setOnAction(event -> {
							final Color selected = colorPicker.getValue();
							// noinspection NumericCastThatLosesPrecision
							this.instantEdit(String.format(
								"#%02x%02x%02x",
								(int)(selected.getRed()*256),
								(int)(selected.getGreen()*256),
								(int)(selected.getBlue()*256)
							));
						});
						this.setText(null);
						this.setGraphic(colorPicker);
					}
				} else if ("[]".equals(type) || VALID_TYPE_VALUES.containsKey(type)) {
					// Lists get a ComboBox.
					if (this.getGraphic() instanceof ComboBox) {
						// ComboBox already exists, so we only need to update the selection.
						@SuppressWarnings("unchecked")
						final ComboBox<String> comboBox = (ComboBox<String>)this.getGraphic();
						comboBox.getSelectionModel().select(pti.getValue());
					} else {
						// ComboBox doesn't exist yet, so create it.
						final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
							"[]".equals(type)
							? pti.getValueType().getValues()
							: VALID_TYPE_VALUES.get(type)
						));
						comboBox.getSelectionModel().select(pti.getValue());
						comboBox.setOnAction(event -> this.instantEdit(comboBox.getValue()));
						this.setText(null);
						this.setGraphic(comboBox);
					}
				} else {
					// Cell is not always editable, so display normal text when not editing
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
