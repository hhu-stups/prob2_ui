package de.prob2.ui.internal;


import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob2.ui.preferences.PrefConstants;
import de.prob2.ui.preferences.ProBPreferences;

public class DynamicTableCell extends TableCell<DynamicCommandItem, String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTableCell.class);
	
	private final ProBPreferences preferences;
	
	public DynamicTableCell(final ProBPreferences preferences) {
		super();
		this.preferences = preferences;
	}
	
	private void setPreferenceValue(final String newValue) {
		this.preferences.setPreferenceValue(((DynamicPreferencesItem) this.getTableRow().getItem()).getName(), newValue);
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
	
	private void changeToTextField(final DynamicPreferencesItem item) {
		final TextField textField = new TextField(item.getValue());
		textField.textProperty().addListener((o, from, to) -> this.setPreferenceValue(to));
		this.setText(null);
		this.setGraphic(textField);
	}
	
	private void changeToText() {
		this.setGraphic(null);
		this.setText(((DynamicPreferencesItem) this.getTableRow().getItem()).getValue());
	}
	
	private void changeToCheckBox(final DynamicPreferencesItem pti) {
		final CheckBox checkBox = new CheckBox();
		checkBox.setSelected("true".equals(pti.getValue()));
		checkBox.setOnAction(event -> this.setPreferenceValue(Boolean.toString(checkBox.isSelected())));
		this.setText(null);
		this.setGraphic(checkBox);
	}
	
	private void changeToColorPicker(final DynamicPreferencesItem pti) {
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
	
	private void changeToComboBox(final DynamicPreferencesItem pti, final String type) {
		final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
			"[]".equals(type)
				? pti.getValueType().getValues()
				: PrefConstants.VALID_TYPE_VALUES.get(type)
		));
		comboBox.getSelectionModel().select(pti.getValue());
		comboBox.setOnAction(event -> this.setPreferenceValue(comboBox.getValue()));
		this.setText(null);
		this.setGraphic(comboBox);
	}
	
	private void changeToItem(final DynamicPreferencesItem pti) {
		if (pti.getValueType() == null) {
			// If there is no value type (for categories for example), just display the value text.
			changeToText();
		} else {
			final String type = pti.getValueType().getType();
			if ("bool".equals(type)) {
				// Booleans get a CheckBox.
				changeToCheckBox(pti);
			} else if ("int".equals(type)) {
				// Integers get a spinner.
				changeToSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, pti.getValue());
			} else if ("nat".equals(type)) {
				// Nonnegative integers get a spinner.
				changeToSpinner(0, Integer.MAX_VALUE, pti.getValue());
			} else if ("nat1".equals(type)) {
				// Positive integers get a spinner.
				changeToSpinner(1, Integer.MAX_VALUE, pti.getValue());
			} else if ("neg".equals(type)) {
				// Nonpositive integers get a spinner.
				changeToSpinner(Integer.MIN_VALUE, 0, pti.getValue());
			} else if ("rgb_color".equals(type)) {
				// Colors get a ColorPicker.
				changeToColorPicker(pti);
			} else if ("[]".equals(type) || PrefConstants.VALID_TYPE_VALUES.containsKey(type)) {
				// Lists get a ComboBox.
				changeToComboBox(pti, type);
			} else {
				// Default to a simple text field if type is unknown.
				changeToTextField(pti);
			}
		}
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (this.getTableRow() != null && this.getTableRow().getItem() != null) {
			// Item is available, which means we can do fancy stuff!
			changeToItem((DynamicPreferencesItem) this.getTableRow().getItem());
		} else {
			// Row or item is null, so display the item text as is.
			this.setGraphic(null);
			this.setText(item);
		}
	}
}
