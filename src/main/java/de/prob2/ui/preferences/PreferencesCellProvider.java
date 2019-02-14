package de.prob2.ui.preferences;

import com.google.inject.Injector;

import de.prob2.ui.dynamic.DynamicTableCell;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesCellProvider<T extends Cell<? extends Object>, R extends Cell<PrefItem>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesCellProvider.class);
	
	private final ReadOnlyObjectProperty<ProBPreferences>  preferences;
	
	private final Injector injector;
	
	private T cell;
	
	private R row;
	
	public PreferencesCellProvider(final T cell, final Injector injector, final ReadOnlyObjectProperty<ProBPreferences> preferences) {
		super();
		this.cell = cell;
		this.injector = injector;
		this.preferences = preferences;
	}
	
	private void setPreferenceValue(final String newValue) {
		if(cell.getItem() == null) {
			return;
		}
		this.preferences.get().setPreferenceValue(row.getItem().getName(), newValue);
		if(cell instanceof DynamicTableCell) {
			injector.getInstance(PreferencesView.class).refresh();
		} else if(cell instanceof MultiTreeTableCell) {
			injector.getInstance(DotView.class).refresh();
			injector.getInstance(ExpressionTableView.class).refresh();
		}
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
		cell.setText(null);
		cell.setGraphic(spinner);
	}
	
	private void changeToTextField(final PrefItem item) {
		final TextField textField = new TextField(item.getValue());
		textField.textProperty().addListener((o, from, to) -> this.setPreferenceValue(to));
		cell.setText(null);
		cell.setGraphic(textField);
	}
	
	private void changeToText() {
		cell.setGraphic(null);
		cell.setText(row.getItem().getValue());
	}
	
	private void changeToCheckBox(final PrefItem pti) {
		final CheckBox checkBox = new CheckBox();
		checkBox.setSelected("true".equals(pti.getValue()));
		checkBox.setOnAction(event -> this.setPreferenceValue(Boolean.toString(checkBox.isSelected())));
		cell.setText(null);
		cell.setGraphic(checkBox);
	}
	
	private void changeToColorPicker(final PrefItem pti) {
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
		cell.setText(null);
		cell.setGraphic(colorPicker);
	}
	
	private void changeToComboBox(final PrefItem pti, final String type) {
		final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
			"[]".equals(type)
				? pti.getValueType().getValues()
				: PrefConstants.VALID_TYPE_VALUES.get(type)
		));
		comboBox.getSelectionModel().select(pti.getValue());
		comboBox.setOnAction(event -> this.setPreferenceValue(comboBox.getValue()));
		cell.setText(null);
		cell.setGraphic(comboBox);
	}
	
	public void changeToItem(final PrefItem pti) {
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
	
	public void updateItem(final String item) {
		if (row != null && row.getItem() != null) {
			// Item is available, which means we can do fancy stuff!
			changeToItem(row.getItem());
		} else {
			// Row or item is null, so display the item text as is.
			cell.setGraphic(null);
			cell.setText(item);
		}
	}
	
	public void setRow(R row) {
		this.row = row;
	}
}
