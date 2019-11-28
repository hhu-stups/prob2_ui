package de.prob2.ui.preferences;

import java.util.Arrays;
import java.util.List;

import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PreferenceValueCell extends TreeTableCell<PrefTreeItem, PrefTreeItem> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferenceValueCell.class);
	
	private final ReadOnlyObjectProperty<ProBPreferences> preferences;
	
	PreferenceValueCell(final ReadOnlyObjectProperty<ProBPreferences> preferences) {
		super();
		
		this.preferences = preferences;
	}
	
	private void setPreferenceValue(final String newValue) {
		this.preferences.get().setPreferenceValue(this.getTreeTableRow().getItem().getName(), newValue);
	}
	
	private void changeToSpinner(final int min, final int max, final String initial) {
		final Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min));
		spinner.setEditable(true);
		spinner.getEditor().textProperty().addListener((o, from, to) -> {
			this.setPreferenceValue(to);
			Integer value;
			try {
				value = spinner.getValueFactory().getConverter().fromString(to);
			} catch (NumberFormatException e) {
				LOGGER.trace("User-entered number is currently invalid", e);
				value = null;
			}
			spinner.getEditor().getStyleClass().remove("text-field-error");
			if (value == null || value < min || value > max) {
				spinner.getEditor().getStyleClass().add("text-field-error");
			} else {
				spinner.getValueFactory().setValue(value);
			}
		});
		spinner.getEditor().setText(initial);
		this.setText(null);
		this.setGraphic(spinner);
	}
	
	private void changeToTextField(final PrefTreeItem.Preference item) {
		final TextField textField = new TextField(item.getValue());
		textField.textProperty().addListener((o, from, to) -> this.setPreferenceValue(to));
		this.setText(null);
		this.setGraphic(textField);
	}
	
	private void changeToCheckBox(final PrefTreeItem.Preference pti) {
		final CheckBox checkBox = new CheckBox();
		checkBox.setSelected("true".equals(pti.getValue()));
		checkBox.setOnAction(event -> this.setPreferenceValue(Boolean.toString(checkBox.isSelected())));
		this.setText(null);
		this.setGraphic(checkBox);
	}
	
	private void changeToColorPicker(final PrefTreeItem.Preference pti) {
		Color color;
		try {
			color = Color.web(pti.getValue());
		} catch (final IllegalArgumentException exc) {
			color = PrefConstants.TK_COLORS.get(pti.getValue().toLowerCase());
			if (color == null) {
				LOGGER.error("Invalid color: {}", pti.getValue(), exc);
				color = Color.color(1.0, 0.0, 1.0);
			}
		}
		final ColorPicker colorPicker = new ColorPicker(color);
		colorPicker.setOnAction(event -> {
			final Color selected = colorPicker.getValue();
			this.setPreferenceValue(String.format(
				"#%02x%02x%02x",
				(int)(selected.getRed()*255),
				(int)(selected.getGreen()*255),
				(int)(selected.getBlue()*255)
			));
		});
		this.setText(null);
		this.setGraphic(colorPicker);
	}
	
	private void changeToComboBox(final PrefTreeItem.Preference pti) {
		final List<String> validValues;
		if (pti.getPreferenceInfo().type instanceof ListPrologTerm) {
			validValues = PrologTerm.atomicStrings((ListPrologTerm)pti.getPreferenceInfo().type);
		} else {
			final String typeName = PrologTerm.atomicString(pti.getPreferenceInfo().type);
			validValues = Arrays.asList(PrefConstants.VALID_TYPE_VALUES.get(typeName));
		}
		final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(validValues));
		comboBox.getSelectionModel().select(pti.getValue());
		comboBox.setOnAction(event -> this.setPreferenceValue(comboBox.getValue()));
		this.setText(null);
		this.setGraphic(comboBox);
	}
	
	@Override
	public void updateItem(final PrefTreeItem item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item instanceof PrefTreeItem.Category) {
			// Empty rows and categories have no value.
			this.setText(null);
			this.setGraphic(null);
		} else if (item instanceof PrefTreeItem.Preference) {
			final PrefTreeItem.Preference pref = (PrefTreeItem.Preference)item;
			if (pref.getPreferenceInfo().type instanceof ListPrologTerm) {
				// Lists get a ComboBox.
				changeToComboBox(pref);
			} else {
				final String type = PrologTerm.atomicString(pref.getPreferenceInfo().type);
				if ("bool".equals(type)) {
					// Booleans get a CheckBox.
					changeToCheckBox(pref);
				} else if ("int".equals(type)) {
					// Integers get a spinner.
					changeToSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, pref.getValue());
				} else if ("nat".equals(type)) {
					// Nonnegative integers get a spinner.
					changeToSpinner(0, Integer.MAX_VALUE, pref.getValue());
				} else if ("nat1".equals(type)) {
					// Positive integers get a spinner.
					changeToSpinner(1, Integer.MAX_VALUE, pref.getValue());
				} else if ("neg".equals(type)) {
					// Nonpositive integers get a spinner.
					changeToSpinner(Integer.MIN_VALUE, 0, pref.getValue());
				} else if ("rgb_color".equals(type)) {
					// Colors get a ColorPicker.
					changeToColorPicker(pref);
				} else if (PrefConstants.VALID_TYPE_VALUES.containsKey(type)) {
					// Named list types get a ComboBox.
					changeToComboBox(pref);
				} else {
					// Default to a simple text field if type is unknown.
					changeToTextField(pref);
				}
			}
		} else {
			throw new AssertionError("Unhandled PrefTreeItem subclass: " + item);
		}
	}
}
