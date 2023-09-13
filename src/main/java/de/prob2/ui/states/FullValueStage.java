package de.prob2.ui.states;

import com.google.inject.Inject;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;
import difflib.DiffUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FullValueStage extends Stage {
	private static final Pattern PRETTIFY_DELIMITERS = Pattern.compile("[\\{\\}\\,]");
	private static final Logger LOGGER = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TabPane tabPane;
	@FXML private Tab formulaTab;
	@FXML private Tab descriptionTab;
	@FXML private Tab currentValueTab;
	@FXML private Tab previousValueTab;
	@FXML private Tab diffTab;
	@FXML private TextArea formulaTextarea;
	@FXML private TextArea descriptionTextarea;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private StyleClassedTextArea diffTextarea;
	@FXML private CheckBox prettifyCheckBox;
	@FXML private CheckBox showFullValueCheckBox;
	@FXML private Button saveAsButton;
	
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;
	
	private final ObjectProperty<StateItem> value;
	private final StateSpace sp;
	
	@Inject
	public FullValueStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final I18n i18n, final MachineLoader machineLoader) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		
		this.value = new SimpleObjectProperty<>(this, "value", null);
		this.sp = machineLoader.getActiveStateSpace();
		stageManager.loadFXML(this, "full_value_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.valueProperty().addListener((o, from, to) -> this.updateValue(to));
		this.prettifyCheckBox.selectedProperty().addListener(o -> this.updateValue(this.getValue()));
		this.showFullValueCheckBox.selectedProperty().addListener(o -> this.updateValue(this.getValue()));
		this.showFullValueCheckBox.setTooltip(new Tooltip(i18n.translate("states.fullValueStage.fullValueTooltip")));
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
		final String prevName = i18n.translate("states.fullValueStage.diff.previousValueName");
		final String curName = i18n.translate("states.fullValueStage.diff.currentValueName");
		final List<String> prevLines = Arrays.asList(pv.split("\n"));
		final List<String> curLines = Arrays.asList(cv.split("\n"));
		final List<String> uniDiffLines = DiffUtils.generateUnifiedDiff(prevName, curName, prevLines, DiffUtils.diff(prevLines, curLines), 3);
		
		this.diffTextarea.clear();
		
		if (uniDiffLines.isEmpty()) {
			this.diffTextarea.appendText(i18n.translate("states.fullValueStage.diff.noDifferencePlaceholder"));
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
			return i18n.translate("states.fullValueStage.value.inactive");
		} else if (value instanceof BVisual2Value.Error) {
			return i18n.translate("states.fullValueStage.value.error", ((BVisual2Value.Error)value).getMessage());
		} else {
			throw new IllegalArgumentException("Cannot display a BVisual2Value of type " + value.getClass());
		}
	}
	
	private void updateValue(final StateItem newValue) {
		if (newValue == null) {
			this.setTitle(null);
			this.formulaTextarea.clear();
			this.descriptionTextarea.clear();
			this.currentValueTextarea.clear();
			this.previousValueTextarea.clear();
			this.diffTextarea.clear();
			return;
		}
		
		if (newValue.getRodinLabels() != null && !newValue.getRodinLabels().isEmpty()) {
			this.setTitle("@" + String.join(";", newValue.getRodinLabels()));
		} else {
			this.setTitle(newValue.getLabel());
		}
		
		this.formulaTextarea.setText(newValue.getLabel());
		this.descriptionTextarea.setText(newValue.getDescription());
		String oldMaxDisplayPref = sp.getCurrentPreference("MAX_DISPLAY_SET");
		Map<String, String> pref = new HashMap<>();
		if(showFullValueCheckBox.isSelected()) {
			pref.put("MAX_DISPLAY_SET", "-1");
			sp.changePreferences(pref);
		}
		final String cv = prettifyIfEnabled(valueToString(newValue.getFormula().evaluate(newValue.getCurrentState())));
		final String pv = newValue.getPreviousState() == null ? "" : prettifyIfEnabled(valueToString(newValue.getFormula().evaluate(newValue.getPreviousState())));
		this.currentValueTextarea.setText(cv);
		this.previousValueTextarea.setText(pv);
		this.updateDiff(cv, pv);
		if(showFullValueCheckBox.isSelected()) {
			pref.put("MAX_DISPLAY_SET", oldMaxDisplayPref);
			sp.changePreferences(pref);
		}
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			fileChooserManager.getPlainTextFilter(),
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
		if (this.getValue().getRodinLabels() != null && !this.getValue().getRodinLabels().isEmpty()) {
			defaultFileName = this.getValue().getRodinLabels().get(0).replace(':', ' ');
		} else if (this.getValue().getLabel().matches("[\\w\\s]+")) {
			defaultFileName = this.getValue().getLabel();
		} else {
			defaultFileName = i18n.translate("states.fullValueStage.saveAs.defaultFileName");
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
			final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected);
			alert.initOwner(this);
			alert.showAndWait();
		}
	}
}
