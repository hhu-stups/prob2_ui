package de.prob2.ui.states;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import difflib.DiffUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FullValueStage extends Stage {
	private static final Pattern PRETTIFY_DELIMITERS = Pattern.compile("[\\{\\}\\,]");
	private static final Logger logger = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TabPane tabPane;
	@FXML private Tab currentValueTab;
	@FXML private Tab previousValueTab;
	@FXML private Tab diffTab;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private StyleClassedTextArea diffTextarea;
	@FXML private ToggleGroup asciiUnicodeGroup;
	@FXML private RadioButton asciiRadio;
	@FXML private RadioButton unicodeRadio;
	@FXML private CheckBox prettifyCheckBox;
	@FXML private Button saveAsButton;
	
	private final StageManager stageManager;
	
	private AsciiUnicodeString currentValue;
	private AsciiUnicodeString previousValue;
	
	@Inject
	public FullValueStage(final StageManager stageManager) {
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "full_value_stage.fxml");
	}
	
	private static String prettify(final String s) {
		final StringBuilder out = new StringBuilder();
		int indentLevel = 0;
		int lastMatchPos = 0;
		Matcher matcher = PRETTIFY_DELIMITERS.matcher(s);
		
		while (matcher.find()) {
			for (int i = 0; i < indentLevel; i++) {
				out.append("\t");
			}
			out.append(s, lastMatchPos, matcher.start());
			lastMatchPos = matcher.end();
			
			switch (matcher.group()) {
				case "{":
					out.append("{\n");
					indentLevel++;
					break;
				
				case "}":
					indentLevel--;
					if (s.charAt(matcher.start()-1) != '{') {
						out.append("\n");
					}
					out.append("}\n");
					break;
				
				case ",":
					out.append(",\n");
					break;
				
				default:
					throw new IllegalStateException("Unhandled delimiter: " + matcher.group());
			}
		}
		
		out.append(s, lastMatchPos, s.length());
		
		return out.toString();
	}
	
	private String prettifyIfEnabled(final String s) {
		return this.prettifyCheckBox.isSelected() ? prettify(s) : s;
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
		final String cv = this.getCurrentValue() == null ? null : prettifyIfEnabled(this.currentValueAsString());
		final String pv = this.getPreviousValue() == null ? null : prettifyIfEnabled(this.previousValueAsString());
		if (cv != null) {
			this.currentValueTextarea.setText(cv);
		}
		if (pv != null) {
			this.previousValueTextarea.setText(pv);
		}
		if (cv != null && pv != null) {
			final List<String> prevLines = Arrays.asList(pv.split("\n"));
			final List<String> curLines = Arrays.asList(cv.split("\n"));
			final List<String> uniDiffLines = DiffUtils.generateUnifiedDiff("", "", prevLines, DiffUtils.diff(prevLines, curLines), 3);
			
			this.diffTextarea.clear();
			if (uniDiffLines.isEmpty()) {
				return;
			}
			
			// Don't display the "file names" in the first two lines
			for (final String line : uniDiffLines.subList(2, uniDiffLines.size())) {
				this.diffTextarea.appendText(line);
				this.diffTextarea.appendText("\n");
				
				final List<String> styleClasses = new ArrayList<>();
				switch (line.charAt(0)) {
					case '@':
						styleClasses.add("coords");
						break;
					
					case '+':
						styleClasses.add("insert");
						break;
					
					case '-':
						styleClasses.add("delete");
						break;
					
					default:
						// No style class
				}
				
				this.diffTextarea.setStyle(
					this.diffTextarea.getLength() - line.length() - 1,
					this.diffTextarea.getLength() - 1,
					styleClasses
				);
			}
		}
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("Text Files", "*.txt"),
			new FileChooser.ExtensionFilter("All Files", "*.*")
		);
		if (diffTab.isSelected()) {
			chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Diff Files", "*.diff"));
			chooser.setInitialFileName(this.getTitle() + ".diff");
		} else {
			chooser.setInitialFileName(this.getTitle() + ".txt");
		}
		final File selected = chooser.showSaveDialog(this);
		if (selected == null) {
			return;
		}
		
		try (
			final OutputStream os = new FileOutputStream(selected);
			final Writer out = new OutputStreamWriter(os, Charset.forName("UTF-8"))
		) {
			final String value;
			if (currentValueTab.isSelected()) {
				value = this.currentValueTextarea.getText();
			} else if (previousValueTab.isSelected()) {
				value = this.previousValueTextarea.getText();
			} else if (diffTab.isSelected()) {
				value = this.diffTextarea.getText();
			} else {
				logger.error("No known tab selected");
				return;
			}
			out.write(value);
		} catch (FileNotFoundException e) {
			logger.error("Could not open file for writing", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file for writing:\n" + e.getMessage()).showAndWait();
		} catch (IOException e) {
			logger.error("Failed to save value to file", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage()).showAndWait();
		}
	}
}
