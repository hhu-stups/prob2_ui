package de.prob2.ui.preferences;

import java.util.List;
import java.util.Locale;

import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;
import de.prob2.ui.internal.ImprovedIntegerSpinnerValueFactory;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TreeTableCell;
import javafx.scene.paint.Color;
import javafx.util.converter.IntegerStringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PreferenceValueCell extends TreeTableCell<PrefTreeItem, PrefTreeItem> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferenceValueCell.class);

	private final ReadOnlyObjectProperty<PreferencesChangeState> state;

	PreferenceValueCell(final ReadOnlyObjectProperty<PreferencesChangeState> state) {
		super();

		this.state = state;
	}

	private void setPreferenceValue(final String name, final String newValue) {
		this.state.get().changePreference(name, newValue);
	}

	private void changeToSpinner(final PrefTreeItem.Preference pti, final int min, final int max) {
		final Spinner<Integer> spinner = new Spinner<>();
		spinner.setEditable(true);
		spinner.getEditor().setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
		spinner.setValueFactory(new ImprovedIntegerSpinnerValueFactory(min, max, 0));
		spinner.getEditor().setText(pti.getValue());

		spinner.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && min <= newValue && newValue <= max) {
				this.setPreferenceValue(
					pti.getName(),
					spinner.getValueFactory().getConverter().toString(newValue)
				);
			}
		});

		this.setText(null);
		this.setGraphic(spinner);
	}

	private void changeToTextField(final PrefTreeItem.Preference item) {
		final TextField textField = new TextField(item.getValue());
		textField.textProperty().addListener((o, from, to) -> this.setPreferenceValue(item.getPreferenceInfo().name, to));
		this.setText(null);
		this.setGraphic(textField);
	}

	private void changeToCheckBox(final PrefTreeItem.Preference pti) {
		final CheckBox checkBox = new CheckBox();
		checkBox.setSelected("true".equals(pti.getValue()));
		checkBox.setOnAction(event -> this.setPreferenceValue(pti.getPreferenceInfo().name, Boolean.toString(checkBox.isSelected())));
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
			this.setPreferenceValue(pti.getPreferenceInfo().name, String.format(Locale.ROOT,
				"#%02x%02x%02x",
				(int) (selected.getRed() * 255),
				(int) (selected.getGreen() * 255),
				(int) (selected.getBlue() * 255)
			));
		});
		this.setText(null);
		this.setGraphic(colorPicker);
	}

	private void changeToComboBox(final PrefTreeItem.Preference pti) {
		final List<String> validValues = PrologTerm.atomsToStrings((ListPrologTerm) pti.getPreferenceInfo().type);
		final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(validValues));
		comboBox.setEditable(true);
		comboBox.getSelectionModel().select(pti.getValue());
		comboBox.setOnAction(event -> this.setPreferenceValue(pti.getPreferenceInfo().name, comboBox.getValue()));
		this.setText(null);
		this.setGraphic(comboBox);
	}

	@Override
	public void updateItem(final PrefTreeItem item, final boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null || item instanceof PrefTreeItem.Category) {
			// Empty rows and categories have no value.
			this.setText(null);
			this.setGraphic(null);
		} else if (item instanceof PrefTreeItem.Preference pref) {
			if (pref.getPreferenceInfo().type instanceof ListPrologTerm) {
				// Lists get a ComboBox.
				changeToComboBox(pref);
			} else {
				final String type = pref.getPreferenceInfo().type.atomToString();
				if ("bool".equals(type)) {
					// Booleans get a CheckBox.
					changeToCheckBox(pref);
				} else if ("int".equals(type)) {
					// Integers get a spinner.
					changeToSpinner(pref, Integer.MIN_VALUE, Integer.MAX_VALUE);
				} else if ("nat".equals(type)) {
					// Nonnegative integers get a spinner.
					changeToSpinner(pref, 0, Integer.MAX_VALUE);
				} else if ("nat1".equals(type)) {
					// Positive integers get a spinner.
					changeToSpinner(pref, 1, Integer.MAX_VALUE);
				} else if ("neg".equals(type)) {
					// Nonpositive integers get a spinner.
					changeToSpinner(pref, Integer.MIN_VALUE, 0);
				} else if ("rgb_color".equals(type)) {
					// Colors get a ColorPicker.
					changeToColorPicker(pref);
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
