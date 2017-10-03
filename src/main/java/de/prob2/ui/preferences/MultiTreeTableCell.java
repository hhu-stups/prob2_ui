package de.prob2.ui.preferences;

import java.io.File;
import java.util.ResourceBundle;

import de.prob2.ui.internal.StageManager;

import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTreeTableCell<S extends PrefTreeItem> extends TreeTableCell<S, String> {
	private static final Logger logger = LoggerFactory.getLogger(MultiTreeTableCell.class);
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	
	public MultiTreeTableCell(final StageManager stageManager, final ResourceBundle bundle) {
		super();
		
		this.stageManager = stageManager;
		this.bundle = bundle;
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
	
	private boolean tryCommitSpinnerValue(final Spinner<Integer> spinner) {
		final int value;
		try {
			value = spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText());
		} catch (final NumberFormatException e) {
			logger.debug("User entered invalid number", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("preferences.view.invalidNumber"), spinner.getEditor().getText())).show();
			return false;
		}
		spinner.getValueFactory().setValue(value);
		this.commitEdit(spinner.getEditor().getText());
		return true;
	}
	
	private void editAsSpinner(final int min, final int max, final String initial) {
		int initialValue;
		try {
			initialValue = Integer.parseInt(initial);
		} catch (NumberFormatException e) {
			logger.debug("Invalid number in initial preference value", e);
			initialValue = 0;
		}
		final Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initialValue));
		spinner.setEditable(true);
		// Commit by pressing Enter in the spinner editor.
		spinner.getEditor().setOnAction(event -> this.tryCommitSpinnerValue(spinner));
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
	}
	
	private void editAsFileChooser(final PrefTreeItem item) {
		final FileChooser fileChooser = new FileChooser();
		final File initialFileDirectory = new File(item.getValue()).getParentFile();
		if (initialFileDirectory != null && initialFileDirectory.exists()) {
			fileChooser.setInitialDirectory(initialFileDirectory);
		}
		final File chosenFile = fileChooser.showOpenDialog(this.getScene().getWindow());
		if (chosenFile == null) {
			this.cancelEdit();
		} else {
			this.commitEdit(chosenFile.getPath());
		}
	}
	
	private void editAsDirectoryChooser(final PrefTreeItem item) {
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
	}
	
	private void editAsText(final PrefTreeItem item) {
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
	
	private void changeToText() {
		this.setGraphic(null);
		this.setText(this.getTreeTableRow().getItem().getValue());
	}
	
	private void changeToCheckBox(final PrefTreeItem pti) {
		if (this.getGraphic() instanceof CheckBox) {
			// CheckBox already exists, so we only need to update the value.
			((CheckBox)this.getGraphic()).setSelected("true".equals(pti.getValue()));
		} else {
			// CheckBox doesn't exist yet, so create it.
			final CheckBox checkBox = new CheckBox();
			checkBox.setSelected("true".equals(pti.getValue()));
			checkBox.setOnAction(event -> this.instantEdit(Boolean.toString(checkBox.isSelected())));
			this.setText(null);
			this.setGraphic(checkBox);
		}
	}
	
	private void changeToColorPicker(final PrefTreeItem pti) {
		Color color;
		try {
			color = Color.web(pti.getValue());
		} catch (final IllegalArgumentException exc) {
			color = PrefConstants.TK_COLORS.get(pti.getValue().toLowerCase());
			if (color == null) {
				logger.error("Invalid color: {}", pti.getValue(), exc);
				color = Color.color(1.0, 0.0, 1.0);
			}
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
					(int)(selected.getRed()*255),
					(int)(selected.getGreen()*255),
					(int)(selected.getBlue()*255)
				));
			});
			this.setText(null);
			this.setGraphic(colorPicker);
		}
	}
	
	private void changeToComboBox(final PrefTreeItem pti, final String type) {
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
					: PrefConstants.VALID_TYPE_VALUES.get(type)
			));
			comboBox.getSelectionModel().select(pti.getValue());
			comboBox.setOnAction(event -> this.instantEdit(comboBox.getValue()));
			this.setText(null);
			this.setGraphic(comboBox);
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
		if (!this.isEditing() || PrefConstants.ALWAYS_EDITABLE.contains(valueType.getType())) {
			return;
		}
		
		this.setText(null);
		switch (valueType.getType()) {
			case "int":
				editAsSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, item.getValue());
				break;
			
			case "nat":
				editAsSpinner(0, Integer.MAX_VALUE, item.getValue());
				break;
			
			case "nat1":
				editAsSpinner(1, Integer.MAX_VALUE, item.getValue());
				break;
			
			case "neg":
				editAsSpinner(Integer.MIN_VALUE, 0, item.getValue());
				break;
			
			case "path":
			case "file_path":
				editAsFileChooser(item);
				break;
			
			case "directory_path":
				editAsDirectoryChooser(item);
				break;
			
			case "string":
			default:
				// Default case for strings and unknown types, simply display a text field.
				editAsText(item);
		}
	}
	
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		// Revert to normal text, unless this is an "always editable" cell.
		if (!PrefConstants.ALWAYS_EDITABLE.contains(this.getTreeTableRow().getItem().getValueType().getType())) {
			changeToText();
		}
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		// If the cell is currently being edited, commit the edit to save the value before it gets out of view.
		if (this.isEditing()) {
			if (this.getGraphic() instanceof TextField) {
				this.commitEdit(((TextField)this.getGraphic()).getText());
			} else if (this.getGraphic() instanceof Spinner<?>) {
				@SuppressWarnings("unchecked")
				final Spinner<Integer> spinner = (Spinner<Integer>)this.getGraphic();
				if (!this.tryCommitSpinnerValue(spinner)) {
					this.cancelEdit();
				}
			}
		}
		
		super.updateItem(item, empty);
		
		if (this.getTreeTableRow() != null && this.getTreeTableRow().getItem() != null) {
			// Item is available, which means we can do fancy stuff!
			final PrefTreeItem pti = this.getTreeTableRow().getItem();
			if (pti.getValueType() == null) {
				// If there is no value type (for categories for example), just display the value text.
				changeToText();
			} else {
				// Handle the special cases for "always editable" cells.
				final String type = pti.getValueType().getType();
				if ("bool".equals(type)) {
					// Booleans get a CheckBox.
					changeToCheckBox(pti);
				} else if ("rgb_color".equals(type)) {
					// Colors get a ColorPicker.
					changeToColorPicker(pti);
				} else if ("[]".equals(type) || PrefConstants.VALID_TYPE_VALUES.containsKey(type)) {
					// Lists get a ComboBox.
					changeToComboBox(pti, type);
				} else {
					// Cell is not always editable, so display normal text when not editing
					changeToText();
				}
			}
		} else {
			// Row or item is null, so display the item text as is.
			this.setGraphic(null);
			this.setText(item);
		}
	}
}
