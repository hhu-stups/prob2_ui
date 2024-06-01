package de.prob2.ui.railml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.hhu.stups.railml2b.load.ImportArguments;
import de.hhu.stups.railml2b.output.DotVisualizer;
import de.hhu.stups.railml2b.utils.SvgConverter;
import de.prob.animator.domainobjects.*;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.*;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static de.hhu.stups.railml2b.utils.FileUtils.replaceOldFile;
import static de.prob.animator.domainobjects.DotVisualizationCommand.EXPRESSION_AS_GRAPH_NAME;
import static de.prob.animator.domainobjects.DotVisualizationCommand.getByName;

@FXMLInjected
@Singleton
public class RailMLInspectDotStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(RailMLInspectDotStage.class);

	@FXML
	private WebView dotView;
	@FXML
	private Label placeholderLabel;
	@FXML
	private TextArea taErrors;
	@FXML
	private Parent errorsView;
	@FXML
	private MenuBar menuBar;
	@FXML
	private Button zoomOutButton;
	@FXML
	private Button zoomInButton;
	@FXML
	private Button incScalingButton;
	@FXML
	private Button decScalingButton;
	@FXML
	private Button refreshDotView;
	@FXML
	private HBox zoomBox;
	@FXML
	private MenuItem zoomResetMenuButton;
	@FXML
	private MenuItem zoomInMenuButton;
	@FXML
	private MenuItem zoomOutMenuButton;

	@FXML
	private ChoiceBox<ImportArguments.VisualisationStrategy> visualisationStrategyChoiceBox;
	@FXML
	private CheckBox balises;
	@FXML
	private CheckBox borders;
	@FXML
	private CheckBox bufferstops;
	@FXML
	private CheckBox crossings;
	@FXML
	private CheckBox derailers;
	@FXML
	private CheckBox operationalpoints;
	@FXML
	private CheckBox levelcrossings;
	@FXML
	private CheckBox signals;
	@FXML
	private CheckBox switches;
	@FXML
	private CheckBox traindetectionelements;
	@FXML
	private CheckBox tvdsections;
	@FXML
	private CheckBox names;
	@FXML
	private ChoiceBox<Language> languageChoiceBox;
	@FXML
	private ChoiceBox<DotEngine> dotEngineChoiceBox;
	private enum Language {EN, NO, DE}
	private enum DotEngine {DOT, NEATO, FDP}
	@FXML
	private HBox dotEngineBox;
	@FXML
	private HBox scalingBox;
	@FXML
	private CheckBox curvedsplines;
	@FXML
	private Button saveButton;
	@FXML
	protected Button cancelButton;

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final BackgroundUpdater updater;
	private final I18n i18n;
	private String modelName = null;
	private Path output;
	private ImportArguments.VisualisationStrategy visualisationStrategy = null;
	private Trace trace;

	private double scalingFactor;
	private String dot;
	private String dotEngine;
	private final ObjectProperty<byte[]> currentDotContent;

	private final KeyCombination zoomResetChar = new KeyCharacterCombination("0", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomResetCode = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomResetKeypad = new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.SHORTCUT_DOWN);
	private final KeyCombination zoomInChar = new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomInCode = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomInKeypad = new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN);
	private final KeyCombination zoomOutChar = new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomOutCode = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomOutKeypad = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN);

	@Inject
	public RailMLInspectDotStage(final StageManager stageManager, final I18n i18n, final FileChooserManager fileChooserManager,
	                             final StopActions stopActions) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.updater = new BackgroundUpdater("railml dot view updater");
		stopActions.add(this.updater::shutdownNow);
		this.i18n = i18n;
		this.trace = null;

		this.scalingFactor = 0.1;
		this.dot = null;
		this.dotEngine = null;
		this.currentDotContent = new SimpleObjectProperty<>(this, "currentDotContent", null);
		stageManager.loadFXML(this, "railml_inspect_dot.fxml");
	}

	@FXML
	public void initialize() {
		saveButton.disableProperty().bind(currentDotContent.isNull());
		//helpButton.setHelpContent("mainmenu.visualisations.graphVisualisation", null);
		dotView.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
			Set<Node> scrollBars = dotView.lookupAll(".scroll-bar");
			for (Node scrollBar : scrollBars) {
				scrollBar.setStyle("-fx-opacity: 0.5;");
			}
		});
		zoomResetMenuButton.setAccelerator(new MultiKeyCombination(zoomResetChar, zoomResetCode, zoomResetKeypad));
		zoomInMenuButton.setAccelerator(new MultiKeyCombination(zoomInChar, zoomInCode, zoomInKeypad));
		zoomOutMenuButton.setAccelerator(new MultiKeyCombination(zoomOutChar, zoomOutCode, zoomOutKeypad));

		visualisationStrategyChoiceBox.getItems().addAll(ImportArguments.VisualisationStrategy.values());
		visualisationStrategyChoiceBox.valueProperty().addListener(((obs,o,n) -> {
			initializeOptionsForStrategy(n);
			visualisationStrategy = n;
		}));
		initializeOptions();
		languageChoiceBox.getItems().addAll(RailMLInspectDotStage.Language.values());
		dotEngineChoiceBox.getItems().addAll(RailMLInspectDotStage.DotEngine.values());
		// TODO: Maybe use ProB preference for dot engine later

		incScalingButton.pressedProperty()
			.addListener((o,f,t) -> scalingFactor += visualisationStrategy.equals(ImportArguments.VisualisationStrategy.D4R) ? 0.001 : 0.1);
		decScalingButton.pressedProperty()
			.addListener((o,f,t) -> scalingFactor -= visualisationStrategy.equals(ImportArguments.VisualisationStrategy.D4R) ? 0.001 : 0.1);

		dotView.visibleProperty().bind(this.updater.runningProperty().not());
		placeholderLabel.visibleProperty().bind(this.updater.runningProperty());
		updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());
		cancelButton.disableProperty().bind(this.updater.runningProperty().not());

		this.setOnShown(e -> stageManager.setMacMenuBar(stageManager.getCurrent(), this.menuBar));
		this.setOnCloseRequest(e -> {
			e.consume();
			this.cancel();
		});
	}

	protected void initializeForArguments(final ImportArguments arguments, final Trace trace) {
		this.modelName = arguments.modelName();
		this.output = arguments.output();
		this.visualisationStrategy = arguments.visualisationStrategy();
		this.trace = trace;
		initializeOptionsForStrategy(visualisationStrategy);
	}

	private void initializeOptionsForStrategy(ImportArguments.VisualisationStrategy strategy) {
		boolean isDotCustomGraph = strategy.equals(ImportArguments.VisualisationStrategy.DOT);

		visualisationStrategyChoiceBox.setValue(strategy);

		balises.setVisible(!isDotCustomGraph); balises.setManaged(!isDotCustomGraph);
		borders.setDisable(isDotCustomGraph);
		bufferstops.setDisable(isDotCustomGraph);
		crossings.setDisable(isDotCustomGraph);
		derailers.setVisible(!isDotCustomGraph); derailers.setManaged(!isDotCustomGraph);
		levelcrossings.setVisible(!isDotCustomGraph); levelcrossings.setManaged(!isDotCustomGraph);
		operationalpoints.setVisible(!isDotCustomGraph); operationalpoints.setManaged(!isDotCustomGraph);
		signals.setVisible(!isDotCustomGraph); signals.setManaged(!isDotCustomGraph);
		switches.setDisable(isDotCustomGraph);
		traindetectionelements.setVisible(!isDotCustomGraph); traindetectionelements.setManaged(!isDotCustomGraph);
		tvdsections.setVisible(!isDotCustomGraph); tvdsections.setManaged(!isDotCustomGraph);

		dotEngineBox.setVisible(isDotCustomGraph); dotEngineBox.setManaged(isDotCustomGraph);
		scalingBox.setVisible(!isDotCustomGraph); scalingBox.setManaged(!isDotCustomGraph);
		curvedsplines.setVisible(isDotCustomGraph); curvedsplines.setManaged(isDotCustomGraph);

		initializeOptions();
		scalingFactor = visualisationStrategy.equals(ImportArguments.VisualisationStrategy.D4R) ? 0.004 : 0.1;
	}

	private void initializeOptions() {
		balises.setSelected(true);
		borders.setSelected(true);
		bufferstops.setSelected(true);
		crossings.setSelected(true);
		derailers.setSelected(true);
		levelcrossings.setSelected(true);
		operationalpoints.setSelected(true);
		signals.setSelected(true);
		switches.setSelected(true);
		traindetectionelements.setSelected(true);
		tvdsections.setSelected(true);
		names.setSelected(true);
		languageChoiceBox.setValue(Language.EN);
		dotEngineChoiceBox.setValue(DotEngine.DOT);
		curvedsplines.setSelected(true);
	}

	protected void visualizeCustomGraph() throws InterruptedException {
		String arguments = DotVisualizer.getArgumentsForGraphDefinition(
				visualisationStrategy,
				balises.isSelected(),
				bufferstops.isSelected(),
				borders.isSelected(),
				crossings.isSelected(),
				derailers.isSelected(),
				operationalpoints.isSelected(),
				levelcrossings.isSelected(),
				signals.isSelected(),
				switches.isSelected(),
				traindetectionelements.isSelected(),
				tvdsections.isSelected(),
				names.isSelected(),
				languageChoiceBox.getValue().toString().toLowerCase(),
				dotEngineChoiceBox.getValue().toString().toLowerCase(),
				curvedsplines.isSelected()
		);
		List<IEvalElement> customGraphFormula = Collections.singletonList(trace.getStateSpace().getModel()
			.parseFormula(visualisationStrategy.getCustomGraphDefinition() + arguments, FormulaExpand.EXPAND));
		this.visualizeCustomGraph(customGraphFormula);
	}

	/**
	 * Same method as in DotView.java, but only for "custom_graph".
	 */
	private void visualizeCustomGraph(final List<IEvalElement> formulas) throws InterruptedException {
		DotVisualizationCommand customGraphItem = getByName(EXPRESSION_AS_GRAPH_NAME, trace);
		final String dotLocal = customGraphItem.getTrace().getStateSpace().getCurrentPreference("DOT");
		final String dotEngineLocal = customGraphItem.getPreferredDotLayoutEngine()
				.orElseGet(() -> customGraphItem.getTrace().getStateSpace().getCurrentPreference("DOT_ENGINE"));
		this.dot = dotLocal;
		this.dotEngine = dotEngineLocal;

		// Make sure dot and engine are set, else react with proper error message
		if (dotLocal == null || dotLocal.isEmpty()) {
			Platform.runLater(() -> {
				this.stageManager.makeAlert(Alert.AlertType.ERROR, "dotty.error.emptyDotPath.header", "dotty.error.emptyDotPath.message").show();
				this.close();
			});
			return;
		} else if (dotEngineLocal == null || dotEngineLocal.isEmpty()) {
			Platform.runLater(() -> {
				this.stageManager.makeAlert(Alert.AlertType.ERROR, "dotty.error.emptyDotEngine.header", "dotty.error.emptyDotEngine.message").show();
				this.close();
			});
			return;
		}

		byte[] dotInput = customGraphItem.visualizeAsDotToBytes(formulas);
		this.currentDotContent.set(dotInput);
		if (!Thread.currentThread().isInterrupted()) {
			final String outputFormat = DotOutputFormat.SVG;
			final byte[] svgData;
			final DotCall dotCall = new DotCall(dotLocal)
					                  .layoutEngine(dotEngineLocal)
					                  .outputFormat(outputFormat)
					                  .input(dotInput);
			try {
				svgData = dotCall.call();
			} catch (ProBError e) {
				LOGGER.error("could not visualize graph with dot (command={}, layoutEngine={}, outputFormat={})", dotLocal, dotEngineLocal, outputFormat, e);
				Platform.runLater(() -> {
					this.stageManager.makeExceptionAlert(e, "dotty.error.dotVisualization.message").show();
				});
				return;
			}

			loadGraph(new String(svgData, StandardCharsets.UTF_8));
		}
	}

	/**
	 * Saves the current visualisation to a temporary SVG file, applies RailMLSvgConverter.convertSvgForVisB
	 * for the current strategy and copies the content of the converted temporary SVG to the final SVG file.
	 * This finishes the task of this stage.
	 */
	@FXML
	private void acceptVisualisation() {
		final Path tempSvgFile;
		try {
			tempSvgFile = Files.createTempFile("railml-", ".svg");
			tempSvgFile.toFile().deleteOnExit();
		} catch (IOException e) {
			throw new ProBError("Failed to create temporary SVG file", e);
		}
		saveConverted(DotOutputFormat.SVG, tempSvgFile);

		try {
			SvgConverter.convertSvgForVisB(tempSvgFile.toString(), visualisationStrategy);
		} catch (Exception e) {
			throw new ProBError("Failed to convert railML SVG file", e);
		}

		final Path finalSvg = output.resolve(modelName + ".svg").toAbsolutePath();
		try {
			replaceOldFile(finalSvg);
			Files.copy(tempSvgFile, finalSvg, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.close();
	}

	/**
	 * After changing elements to be visualised or Dot options, the visualisation can be reloaded using this method.
	 */
	@FXML
	private void refreshDotView () {
		this.updater.execute(() -> {
			try {
				visualizeCustomGraph();
			} catch (InterruptedException e) {
				LOGGER.info("RailML Visualization interrupted", e);
				Thread.currentThread().interrupt();
			} catch (ProBError e) {
				handleProBError(e);
			} catch (Exception e) {
				if (e.getCause() instanceof ProBError) {
					handleProBError((ProBError) e.getCause());
				} else if (e.getCause() instanceof BCompoundException) {
					handleProBError(new ProBError((BCompoundException) e.getCause()));
				} else {
					LOGGER.error("RailML Visualization failed", e);
					Platform.runLater(() -> {
						taErrors.setText(e.getMessage());
						errorsView.setVisible(true);
					});
				}
			}
		});
	}

	private void handleProBError(ProBError e)  {
		LOGGER.error("Visualization failed with ProBError", e);
		Platform.runLater(() -> {
			taErrors.setText(e.getMessage());
			errorsView.setVisible(true);
		});
	}

	@FXML
	private void interrupt() {
		if (trace != null && trace.getStateSpace() != null)
			trace.getStateSpace().sendInterrupt();
	}

	@FXML
	private void cancel() {
		boolean abortImport = confirmAbortImport();
		if (abortImport) {
			updater.cancel(true);
			this.interrupt();
			this.close();
		}
	}

	private boolean confirmAbortImport() {
		final Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
				"railml.inspectDot.alerts.confirmAbortImport.header",
				"railml.inspectDot.alerts.confirmAbortImport.content");
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}

	/* -------------------------------
		Helper methods for saving taken from DotView.java:
	 */

	private void loadGraph(final String svgContent) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			if (!thread.isInterrupted()) {
				dotView.getEngine().loadContent("<center>" + svgContent + "</center>");
			}
		});
	}

	@FXML
	private void save() {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter svgFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.svg", "svg");
		FileChooser.ExtensionFilter pngFilter = fileChooserManager.getPngFilter();
		FileChooser.ExtensionFilter dotFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.dot", "dot");
		FileChooser.ExtensionFilter pdfFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.pdf", "pdf");
		fileChooser.getExtensionFilters().setAll(svgFilter, pngFilter, dotFilter, pdfFilter);
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, this.getScene().getWindow());
		if (path == null) {
			return;
		}
		FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
		if (selectedFilter.equals(dotFilter)) {
			saveDot(path);
		} else {
			final String format = getTargetFormat(selectedFilter, svgFilter, pngFilter, pdfFilter);
			saveConverted(format, path);
		}
	}

	private String getTargetFormat(FileChooser.ExtensionFilter selectedFilter, FileChooser.ExtensionFilter svgFilter, FileChooser.ExtensionFilter pngFilter, FileChooser.ExtensionFilter pdfFilter) {
		if (selectedFilter.equals(svgFilter)) {
			return DotOutputFormat.SVG;
		} else if (selectedFilter.equals(pngFilter)) {
			return DotOutputFormat.PNG;
		} else if (selectedFilter.equals(pdfFilter)) {
			return DotOutputFormat.PDF;
		} else {
			throw new RuntimeException("Target Format cannot be extracted from selected filter: " + selectedFilter);
		}
	}

	private void saveDot(final Path path) {
		try {
			Files.write(path, this.currentDotContent.get());
		} catch (IOException e) {
			LOGGER.error("Failed to save Dot", e);
		}
	}

	private void saveConverted(String format, final Path path) {
		try {
			Files.write(path, new DotCall(this.dot)
				.layoutEngine(this.dotEngine)
				.outputFormat(format)
				.input(this.currentDotContent.get())
				.call());
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to save file converted from dot", e);
		}
	}

	/* -------------------------------
		Helper methods for zoom, scroll, and placeholder taken from DotView.java:
	 */

	private void updatePlaceholderLabel() {
		final String text;
		if (this.updater.isRunning()) {
			text = i18n.translate("dynamic.placeholder.inProgress");
		} else {
			text = "";
		}
		placeholderLabel.setText(text);
	}

	@FXML
	private void defaultSize() {
		dotView.setZoom(1);
	}

	@FXML
	private void zoomIn() {
		zoomByFactor(1.15);
		adjustScroll();
	}

	@FXML
	private void zoomOut() {
		zoomByFactor(0.85);
		adjustScroll();
	}

	private void zoomByFactor(double factor) {
		dotView.setZoom(dotView.getZoom() * factor);
	}

	private void adjustScroll() {
		Set<Node> nodes = dotView.lookupAll(".scroll-bar");
		double x = 0.0;
		double y = 0.0;
		for (final Node node : nodes) {
			if (node instanceof ScrollBar) {
				ScrollBar sb = (ScrollBar) node;
				if (sb.getOrientation() == Orientation.VERTICAL) {
					x = sb.getPrefHeight() / 2;
				} else {
					y = sb.getPrefWidth() / 2;
				}
			}
		}
		dotView.getEngine().executeScript("window.scrollBy(" + x + "," + y + ")");
	}
}
