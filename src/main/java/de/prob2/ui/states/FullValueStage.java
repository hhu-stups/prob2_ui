package de.prob2.ui.states;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullValueStage extends Stage {

	private static final Pattern PLAIN_LABEL_PATTERN = Pattern.compile("[\\w\\s]+");
	private static final Logger LOGGER = LoggerFactory.getLogger(FullValueStage.class);

	private final StatesView statesView;
	private final Injector injector;

	@FXML
	private TabPane tabPane;
	@FXML
	private Tab formulaTab;
	@FXML
	private Tab labelTab;
	@FXML
	private Tab descriptionTab;
	@FXML
	private Tab currentValueTab;
	@FXML
	private Tab previousValueTab;
	@FXML
	private Tab diffTab;
	@FXML
	private TextArea formulaTextarea;
	@FXML
	private TextArea labelTextarea;
	@FXML
	private TextArea descriptionTextarea;
	@FXML
	private TextArea currentValueTextarea;
	@FXML
	private TextArea previousValueTextarea;
	@FXML
	private StyleClassedTextArea diffTextarea;
	@FXML
	private CheckBox prettifyCheckBox;
	@FXML
	private CheckBox showFullValueCheckBox;
	@FXML
	private Button saveAsButton;

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;

	private final ObjectProperty<StateItem> value;
	private final StateSpace sp;

	@Inject
	public FullValueStage(final StageManager stageManager, final Injector injector, final FileChooserManager fileChooserManager, final I18n i18n, final MachineLoader machineLoader, StatesView statesView) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.statesView = statesView;
		this.value = new SimpleObjectProperty<>(this, "value", null);
		this.sp = machineLoader.getActiveStateSpace();
		this.injector = injector;
		stageManager.loadFXML(this, "full_value_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.valueProperty().addListener((o, from, to) -> this.updateValue(to));
		this.prettifyCheckBox.selectedProperty().addListener(o -> this.updateValue(this.getValue()));
		this.showFullValueCheckBox.selectedProperty().addListener(o -> this.updateValue(this.getValue()));
		this.showFullValueCheckBox.setTooltip(new Tooltip(i18n.translate("states.fullValueStage.fullValueTooltip")));
		this.tabPane.getSelectionModel().select(this.currentValueTab);

		final MenuItem visualizeExpressionAsGraphItem = new MenuItem(
			i18n.translate("states.statesView.contextMenu.items.visualizeExpressionGraph"));
		visualizeExpressionAsGraphItem.setOnAction(event -> {
			try {
				String visualizedFormula = statesView.getFormulaForVisualization(value.getValue());
				DotView formulaStage = injector.getInstance(DotView.class);
				formulaStage.show();
				formulaStage.toFront();
				if (value.getValue().getType().equals(ExpandedFormula.FormulaType.EXPRESSION)) {
					formulaStage.visualizeFormulaAsGraph(visualizedFormula);
				} else {
					formulaStage.visualizeFormulaAsTree(visualizedFormula);
				}
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		});

//		final MenuItem visualizeExpressionAsTreeItem = new MenuItem(
//			i18n.translate("states.statesView.contextMenu.items.visualizeExpressionTree"));
//		visualizeExpressionAsTreeItem.setOnAction(event -> {
//			try {
//				String visualizedFormula = statesView.getFormulaForVisualization(value.getValue());
//				DotView formulaStage = injector.getInstance(DotView.class);
//				formulaStage.show();
//				formulaStage.toFront();
//				System.err.println(this.value.getValue().getType());
//					formulaStage.visualizeFormulaAsTree(visualizedFormula);
//
//			} catch (EvaluationException | ProBError e) {
//				LOGGER.error("Could not visualize formula", e);
//				final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
//				alert.initOwner(this.getScene().getWindow());
//				alert.showAndWait();
//			}
//		});

		final MenuItem visualizeExpressionAsTableItem = new MenuItem(
			i18n.translate("states.statesView.contextMenu.items.visualizeExpressionTable"));
		visualizeExpressionAsTableItem.setOnAction(event -> {
			try {
				String visualizedFormula = statesView.getFormulaForVisualization(value.getValue());
				if (ExpandedFormula.FormulaType.PREDICATE == value.getValue().getFormula().expandStructureNonrecursive().getType()) {
					visualizedFormula = String.format(Locale.ROOT, "bool(%s)", visualizedFormula);
				}
				ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
				expressionTableView.show();
				expressionTableView.toFront();
				expressionTableView.visualizeExpression(visualizedFormula);
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		});

		currentValueTextarea.contextMenuProperty().setValue(new ContextMenu(visualizeExpressionAsGraphItem, visualizeExpressionAsTableItem));
	}

	private static String prettify(final String s) {
		int len = s.length();
		StringBuilder out = new StringBuilder(len);

		// TODO: ignore quoted or escaped brackets/commas
		int indentLevel = 0;

		char c;
		for (int i = 0; i < len; i++) {
			c = s.charAt(i);

			switch (c) {
				case '{':
				case '[':
					indentLevel++;
					out.append(c);
					out.append('\n').append("\t".repeat(indentLevel));
					break;
				case '}':
				case ']':
					if (indentLevel > 0) {
						indentLevel--;
					}
					out.append('\n').append("\t".repeat(indentLevel));
					out.append(c);
					break;
				case ',':
					out.append(c);
					out.append('\n').append("\t".repeat(indentLevel));
					break;
				default:
					out.append(c);
			}
		}

		return out.toString();
	}

	private String prettifyIfEnabled(final String s) {
		return this.prettifyCheckBox.isSelected() ? prettify(s) : s;
	}

	public void bindCheckboxes(BooleanProperty prettifyProperty, BooleanProperty showFullValueProperty) {
		this.prettifyCheckBox.selectedProperty().bindBidirectional(prettifyProperty);
		this.showFullValueCheckBox.selectedProperty().bindBidirectional(showFullValueProperty);
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
		final List<String> uniDiffLines = UnifiedDiffUtils.generateUnifiedDiff(prevName, curName, prevLines, DiffUtils.diff(prevLines, curLines), 3);

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
			return String.valueOf(((BVisual2Value.PredicateValue) value).getValue());
		} else if (value instanceof BVisual2Value.ExpressionValue) {
			return ((BVisual2Value.ExpressionValue) value).getValue();
		} else if (value instanceof BVisual2Value.Inactive) {
			return i18n.translate("states.fullValueStage.value.inactive");
		} else if (value instanceof BVisual2Value.Error) {
			return i18n.translate("states.fullValueStage.value.error", ((BVisual2Value.Error) value).getMessage());
		} else {
			throw new IllegalArgumentException("Cannot display a BVisual2Value of type " + value.getClass());
		}
	}

	private void updateValue(final StateItem newValue) {
		if (newValue == null) {
			this.setTitle(null);
			this.formulaTextarea.clear();
			this.labelTextarea.setDisable(true);
			this.labelTextarea.clear();
			this.descriptionTextarea.clear();
			this.currentValueTextarea.clear();
			this.previousValueTextarea.clear();
			this.diffTextarea.clear();
			return;
		}

		if (newValue.getRodinLabels() != null && !newValue.getRodinLabels().isEmpty()) {
			String label = "@" + String.join(";", newValue.getRodinLabels());
			this.setTitle(label);
			if (this.prettifyCheckBox.isSelected()) {
				this.labelTextarea.setText(newValue.getRodinLabels().stream()
					                           .map(l -> "@" + l)
					                           .collect(Collectors.joining("\n")));
			} else {
				this.labelTextarea.setText(label);
			}

			this.labelTab.setDisable(false);
		} else {
			this.setTitle(newValue.getLabel());
			this.labelTab.setDisable(true);
			this.labelTextarea.clear();
		}

		this.formulaTextarea.setText(newValue.getLabel());
		this.descriptionTextarea.setText(newValue.getDescription());
		String oldMaxDisplayPref = sp.getCurrentPreference("MAX_DISPLAY_SET");
		Map<String, String> pref = new HashMap<>();
		if (showFullValueCheckBox.isSelected()) {
			pref.put("MAX_DISPLAY_SET", "-1");
			sp.changePreferences(pref);
		}
		final String cv = prettifyIfEnabled(valueToString(newValue.getFormula().evaluate(newValue.getCurrentState())));
		final String pv = newValue.getPreviousState() == null ? "" : prettifyIfEnabled(valueToString(newValue.getFormula().evaluate(newValue.getPreviousState())));
		this.currentValueTextarea.setText(cv);
		this.previousValueTextarea.setText(pv);
		this.updateDiff(cv, pv);
		if (showFullValueCheckBox.isSelected()) {
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
		} else if (PLAIN_LABEL_PATTERN.matcher(this.getValue().getLabel()).matches()) {
			defaultFileName = this.getValue().getLabel();
		} else {
			defaultFileName = i18n.translate("states.fullValueStage.saveAs.defaultFileName");
		}

		chooser.setInitialFileName(defaultFileName + defaultExtension);
		final Path selected = fileChooserManager.showSaveFileChooser(chooser, null, this);
		if (selected == null) {
			return;
		}

		final String value;
		if (formulaTab.isSelected()) {
			value = this.formulaTextarea.getText();
		} else if (labelTab.isSelected()) {
			value = this.labelTextarea.getText();
		} else if (descriptionTab.isSelected()) {
			value = this.descriptionTextarea.getText();
		} else if (currentValueTab.isSelected()) {
			value = this.currentValueTextarea.getText();
		} else if (previousValueTab.isSelected()) {
			value = this.previousValueTextarea.getText();
		} else if (diffTab.isSelected()) {
			value = this.diffTextarea.getText();
		} else {
			throw new AssertionError("No known tab selected");
		}

		try {
			Files.writeString(selected, value);
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected);
			alert.initOwner(this);
			alert.showAndWait();
		}
	}
}
