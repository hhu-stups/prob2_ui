package de.prob2.ui.states;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullValueStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TabPane tabPane;
	@FXML private Tab previousValueTab;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private ToggleGroup asciiUnicodeGroup;
	@FXML private RadioButton asciiRadio;
	@FXML private RadioButton unicodeRadio;
	@FXML private Button saveAsButton;
	
	private AsciiUnicodeString currentValue;
	private AsciiUnicodeString previousValue;
	
	@Inject
	public FullValueStage(final FXMLLoader loader) {
		loader.setLocation(getClass().getResource("full_value_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}
	
	public AsciiUnicodeString getCurrentValue() {
		return this.currentValue;
	}
	
	public void setCurrentValue(final AsciiUnicodeString currentValue) {
		Objects.requireNonNull(currentValue);
		this.currentValue = currentValue;
		this.updateText();
	}
	
	public String currentValueAsString() {
		return asciiRadio.isSelected() ? this.getCurrentValue().toAscii() : this.getCurrentValue().toUnicode();
	}
	
	public AsciiUnicodeString getPreviousValue() {
		return this.previousValue;
	}
	
	public void setPreviousValue(final AsciiUnicodeString previousValue) {
		Objects.requireNonNull(previousValue);
		this.previousValue = previousValue;
		this.updateText();
	}
	
	public String previousValueAsString() {
		return asciiRadio.isSelected() ? this.getPreviousValue().toAscii() : this.getPreviousValue().toUnicode();
	}
	
	@FXML
	private void updateText() {
		if (this.getCurrentValue() != null) {
			this.currentValueTextarea.setText(this.currentValueAsString());
		}
		if (this.getPreviousValue() != null) {
			this.previousValueTextarea.setText(this.previousValueAsString());
		}
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("Text Files", "*.txt")
		);
		chooser.setInitialFileName(this.getTitle() + ".txt");
		final File selected = chooser.showSaveDialog(this);
		if (selected == null) {
			return;
		}
		
		try (final Writer out = new OutputStreamWriter(new FileOutputStream(selected), Charset.forName("UTF-8"))) {
			final String value;
			if (previousValueTab.isSelected()) {
				value = this.previousValueAsString();
			} else {
				value = this.currentValueAsString();
			}
			out.write(value);
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
			logger.error("Failed to save value to file", e);
			new Alert(Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage()).showAndWait();
		}
	}
}
