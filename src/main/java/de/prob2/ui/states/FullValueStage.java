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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import difflib.DiffUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.fxmisc.richtext.StyleClassedTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullValueStage extends Stage {
	private static final Pattern PRETTIFY_DELIMITERS = Pattern.compile("[\\{\\}\\,]");
	private static final Logger LOGGER = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TabPane tabPane;
	@FXML private Tab currentValueTab;
	@FXML private Tab previousValueTab;
	@FXML private Tab diffTab;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private StyleClassedTextArea diffTextarea;
	@FXML private CheckBox prettifyCheckBox;
	@FXML private Button saveAsButton;
	
	private final StageManager stageManager;
	
	private final StringProperty currentValue;
	private final StringProperty previousValue;
	private final BooleanProperty formattingEnabled;
	
	@Inject
	public FullValueStage(final StageManager stageManager) {
		this.stageManager = stageManager;
		this.currentValue = new SimpleStringProperty(this, "currentValue", null);
		this.previousValue = new SimpleStringProperty(this, "previousValue", null);
		this.formattingEnabled = new SimpleBooleanProperty(this, "formattingEnabled", true);
		stageManager.loadFXML(this, "full_value_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.prettifyCheckBox.visibleProperty().bind(this.formattingEnabledProperty());
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
	
	public StringProperty currentValueProperty() {
		return this.currentValue;
	}
	
	public String getCurrentValue() {
		return this.currentValueProperty().get();
	}
	
	public void setCurrentValue(final String currentValue) {
		this.currentValueProperty().set(currentValue);
		this.updateTabs();
	}
	
	public StringProperty previousValueProperty() {
		return this.previousValue;
	}
	
	public String getPreviousValue() {
		return this.previousValueProperty().get();
	}
	
	public void setPreviousValue(final String previousValue) {
		this.previousValueProperty().set(previousValue);
		this.updateTabs();
	}
	
	public BooleanProperty formattingEnabledProperty() {
		return this.formattingEnabled;
	}
	
	public boolean isFormattingEnabled() {
		return this.formattingEnabledProperty().get();
	}
	
	public void setFormattingEnabled(final boolean formattingEnabled) {
		this.formattingEnabledProperty().set(formattingEnabled);
	}
	
	private void updateDiff(final String cv, final String pv) {
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
	
	@FXML
	private void updateTabs() {
		final String cv = this.getCurrentValue() == null ? null : prettifyIfEnabled(this.getCurrentValue());
		final String pv = this.getPreviousValue() == null ? null : prettifyIfEnabled(this.getCurrentValue());
		if (cv != null) {
			this.currentValueTextarea.setText(cv);
		}
		if (pv != null) {
			this.previousValueTextarea.setText(pv);
		}
		if (cv != null && pv != null) {
			this.updateDiff(cv, pv);
		}
		
		this.currentValueTab.setDisable(cv == null);
		this.previousValueTab.setDisable(pv == null);
		this.diffTab.setDisable(cv == null || pv == null);
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
				LOGGER.error("No known tab selected");
				return;
			}
			out.write(value);
		} catch (FileNotFoundException e) {
			LOGGER.error("Could not open file for writing", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file for writing:\n" + e.getMessage()).showAndWait();
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage()).showAndWait();
		}
	}
}
