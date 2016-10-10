package de.prob2.ui.states;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

import com.google.inject.Inject;

import de.prob.unicode.UnicodeTranslator;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullValueStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TextArea textarea;
	@FXML private ToggleGroup asciiUnicodeGroup;
	@FXML private RadioButton asciiRadio;
	@FXML private RadioButton unicodeRadio;
	@FXML private Button saveAsButton;
	
	private String asciiText;
	private String unicodeText;
	
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
	
	public String getAsciiText() {
		return this.asciiText;
	}
	
	public void setAsciiText(final String asciiText) {
		Objects.requireNonNull(asciiText);
		this.asciiText = asciiText;
		this.unicodeText = UnicodeTranslator.toUnicode(asciiText);
		this.updateText();
	}
	
	public String getUnicodeText() {
		return this.unicodeText;
	}
	
	public void setUnicodeText(final String unicodeText) {
		Objects.requireNonNull(unicodeText);
		this.unicodeText = unicodeText;
		this.asciiText = UnicodeTranslator.toAscii(unicodeText);
		this.updateText();
	}
	
	public String getText() {
		return asciiRadio.isSelected() ? this.getAsciiText() : this.getUnicodeText();
	}
	
	@FXML
	private void updateText() {
		this.textarea.setText(this.getText());
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
			out.write(this.getText());
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
			logger.error("Failed to save value to file", e);
			new Alert(Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage()).showAndWait();
		}
	}
}
