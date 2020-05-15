package de.prob2.ui.states;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.BVisual2Value;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;

import difflib.DiffUtils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
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
	@FXML private Tab formulaTab;
	@FXML private Tab currentValueTab;
	@FXML private Tab previousValueTab;
	@FXML private Tab diffTab;
	@FXML private TextArea formulaTextarea;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private StyleClassedTextArea diffTextarea;
	@FXML private CheckBox prettifyCheckBox;
	@FXML private Button saveAsButton;
	
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	
	private final ObjectProperty<StateItem> value;
	
	@Inject
	public FullValueStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		
		this.value = new SimpleObjectProperty<>(this, "value", null);
		
		stageManager.loadFXML(this, "full_value_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.valueProperty().addListener((o, from, to) -> this.updateValue(to));
		this.prettifyCheckBox.selectedProperty().addListener(o -> this.updateValue(this.getValue()));
		this.tabPane.getSelectionModel().select(this.currentValueTab);
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
	
	public ObjectProperty<StateItem> valueProperty() {
		return this.value;
	}
	
	public StateItem getValue() {
		return this.valueProperty().get();
	}
	
	public void setValue(final StateItem value) {
		this.valueProperty().set(value);
	}
	
	private void updateDiff(final String cv, final String pv) {
		final String prevName = bundle.getString("states.fullValueStage.diff.previousValueName");
		final String curName = bundle.getString("states.fullValueStage.diff.currentValueName");
		final List<String> prevLines = Arrays.asList(pv.split("\n"));
		final List<String> curLines = Arrays.asList(cv.split("\n"));
		final List<String> uniDiffLines = DiffUtils.generateUnifiedDiff(prevName, curName, prevLines, DiffUtils.diff(prevLines, curLines), 3);
		
		this.diffTextarea.clear();
		
		if (uniDiffLines.isEmpty()) {
			this.diffTextarea.appendText(bundle.getString("states.fullValueStage.diff.noDifferencePlaceholder"));
		} else {
			for (final String line : uniDiffLines) {
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
				
				this.diffTextarea.append(line + "\n", styleClasses);
			}
		}
	}
	
	private String valueToString(final BVisual2Value value) {
		Objects.requireNonNull(value, "value");
		
		if (value instanceof BVisual2Value.PredicateValue) {
			return String.valueOf(((BVisual2Value.PredicateValue)value).getValue());
		} else if (value instanceof BVisual2Value.ExpressionValue) {
			return ((BVisual2Value.ExpressionValue)value).getValue();
		} else if (value instanceof BVisual2Value.Inactive) {
			return bundle.getString("states.fullValueStage.value.inactive");
		} else if (value instanceof BVisual2Value.Error) {
			return String.format(
				bundle.getString("states.fullValueStage.value.error"),
				((BVisual2Value.Error)value).getMessage()
			);
		} else {
			throw new IllegalArgumentException("Cannot display a BVisual2Value of type " + value.getClass());
		}
	}
	
	private void updateValue(final StateItem newValue) {
		if (newValue == null) {
			this.setTitle(null);
			this.formulaTextarea.clear();
			this.currentValueTextarea.clear();
			this.previousValueTextarea.clear();
			this.diffTextarea.clear();
			return;
		}
		
		this.setTitle(newValue.getLabel());
		this.formulaTextarea.setText(newValue.getLabel());
		final String cv = prettifyIfEnabled(valueToString(newValue.getCurrentValue()));
		final String pv = prettifyIfEnabled(valueToString(newValue.getPreviousValue()));
		this.currentValueTextarea.setText(cv);
		this.previousValueTextarea.setText(pv);
		this.updateDiff(cv, pv);
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.text", "txt"),
			fileChooserManager.getAllExtensionsFilter()
		);
		final String defaultExtension;
		if (diffTab.isSelected()) {
			chooser.getExtensionFilters().add(0, fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.diff", "diff"));
			defaultExtension = ".diff";
		} else {
			defaultExtension = ".txt";
		}
		final String defaultFileName;
		if (this.getValue().getLabel().matches("[\\w\\s]+")) {
			defaultFileName = this.getValue().getLabel();
		} else {
			defaultFileName = bundle.getString("states.fullValueStage.saveAs.defaultFileName");
		}
		chooser.setInitialFileName(defaultFileName + defaultExtension);
		final Path selected = fileChooserManager.showSaveFileChooser(chooser, null, this);
		if (selected == null) {
			return;
		}
		
		try (final Writer out = Files.newBufferedWriter(selected)) {
			final String value;
			if (formulaTab.isSelected()) {
				value = this.formulaTextarea.getText();
			} else if (currentValueTab.isSelected()) {
				value = this.currentValueTextarea.getText();
			} else if (previousValueTab.isSelected()) {
				value = this.previousValueTextarea.getText();
			} else if (diffTab.isSelected()) {
				value = this.diffTextarea.getText();
			} else {
				throw new AssertionError("No known tab selected");
			}
			out.write(value);
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected).showAndWait();
		}
	}
}
