package de.prob2.ui.dynamic.plantuml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.PlantUmlVisualizationCommand;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicFormulaStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.MultiKeyCombination;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PlantUmlView extends DynamicFormulaStage<PlantUmlVisualizationCommand, PlantUmlFormulaTask> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlantUmlView.class);

	@FXML
	private WebView pumlView;

	@FXML
	private MenuBar menuBar;

	@FXML
	private Button zoomOutButton;

	@FXML
	private Button zoomInButton;

	@FXML
	private HBox zoomBox;

	@FXML
	private Button saveButton;

	@FXML
	private HelpButton helpButton;

	@FXML
	private MenuItem zoomResetMenuButton;

	@FXML
	private MenuItem zoomInMenuButton;

	@FXML
	private MenuItem zoomOutMenuButton;

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final JavaLocator javaLocator;
	private final PlantUmlLocator plantUmlLocator;

	private final ObjectProperty<byte[]> currentPumlContent;

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
	public PlantUmlView(final StageManager stageManager, final Provider<DynamicPreferencesStage> preferencesStageProvider, final CurrentTrace currentTrace,
	                    final CurrentProject currentProject, final I18n i18n, final FileChooserManager fileChooserManager, final StopActions stopActions, JavaLocator javaLocator, PlantUmlLocator plantUmlLocator) {
		super(preferencesStageProvider, stageManager, currentTrace, currentProject, i18n, stopActions, "PlantUml Visualizer");
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.javaLocator = javaLocator;
		this.plantUmlLocator = plantUmlLocator;
		this.currentPumlContent = new SimpleObjectProperty<>(this, "currentPumlContent", null);
		stageManager.loadFXML(this, "puml_view.fxml");
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();
		stageManager.setMacMenuBar(this, this.menuBar);
		saveButton.disableProperty().bind(currentPumlContent.isNull());
		helpButton.setHelpContent("mainmenu.visualisations.graphVisualisation", null);
		pumlView.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
			Set<Node> scrollBars = pumlView.lookupAll(".scroll-bar");
			for (Node scrollBar : scrollBars) {
				scrollBar.setStyle("-fx-opacity: 0.5;");
			}
		});
		zoomResetMenuButton.setAccelerator(new MultiKeyCombination(zoomResetChar, zoomResetCode, zoomResetKeypad));
		zoomInMenuButton.setAccelerator(new MultiKeyCombination(zoomInChar, zoomInCode, zoomInKeypad));
		zoomOutMenuButton.setAccelerator(new MultiKeyCombination(zoomOutChar, zoomOutCode, zoomOutKeypad));

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			tvFormula.itemsProperty().unbind();
			Machine machine = currentProject.getCurrentMachine();
			if (machine == null || to == null) {
				tvFormula.setItems(FXCollections.observableArrayList());
			} else {
				tvFormula.setItems(machine.getMachineProperties().getPlantUmlFormulaTasksByCommand(to.getCommand()));
			}
		});
	}

	@Override
	protected List<PlantUmlVisualizationCommand> getCommandsWithTrace(final Trace trace) {
		return PlantUmlVisualizationCommand.getAll(trace);
	}

	@Override
	protected void visualizeInternal(final PlantUmlVisualizationCommand item, final List<IEvalElement> formulas) throws InterruptedException {
		String javaExecutable = this.javaLocator.getJavaExecutable();
		Optional<Path> optPlantUmlJar = this.plantUmlLocator.findPlantUmlJar();
		if (optPlantUmlJar.isEmpty()) {
			Platform.runLater(() -> {
				this.stageManager.makeAlert(
					Alert.AlertType.ERROR,
					"plantuml.error.noPlantUml.header",
					"plantuml.error.noPlantUml.message",
					"https://plantuml.com/download",
					this.plantUmlLocator.getDirectory()
				).show();
				this.close();
			});
			return;
		}
		Path plantUmlJar = optPlantUmlJar.get();

		byte[] pumlInput = item.visualizeAsPlantUmlToBytes(formulas);
		this.currentPumlContent.set(pumlInput);
		if (!Thread.currentThread().isInterrupted()) {
			final String outputFormat = PlantUmlCall.SVG;
			final byte[] svgData;
			final PlantUmlCall pumlCall = new PlantUmlCall(javaExecutable, plantUmlJar)
				                        .outputFormat(outputFormat)
				                        .input(pumlInput);
			try {
				svgData = pumlCall.call();
			} catch (ProBError e) {
				LOGGER.error("could not visualize graph with plantuml (java={} plantuml={}, outputFormat={})", javaExecutable, plantUmlJar, outputFormat, e);
				Platform.runLater(() -> this.stageManager.makeExceptionAlert(
						e,
						"plantuml.error.plantumlVisualization.header",
						"plantuml.error.plantumlVisualization.message",
						javaExecutable,
						plantUmlJar,
						outputFormat
				).show());
				return;
			}

			loadGraph(new String(svgData, StandardCharsets.UTF_8));
		}
	}

	private void loadGraph(final String svgContent) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			if (!thread.isInterrupted()) {
				pumlView.getEngine().loadContent("<center>" + svgContent + "</center>");
				placeholderLabel.setVisible(false);
				taErrors.clear();
				errorsView.setVisible(false);
				pumlView.setVisible(true);
				errors.clear();
				taFormula.getErrors().clear();
			}
		});
	}

	@FXML
	private void save() {
		throw new UnsupportedOperationException();
		/*final FileChooser fileChooser = new FileChooser();
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
		}*/
	}

	/*private String getTargetFormat(FileChooser.ExtensionFilter selectedFilter, FileChooser.ExtensionFilter svgFilter, FileChooser.ExtensionFilter pngFilter, FileChooser.ExtensionFilter pdfFilter) {
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
			Files.write(path, this.currentPumlContent.get());
		} catch (IOException e) {
			LOGGER.error("Failed to save Dot", e);
		}
	}

	private void saveConverted(String format, final Path path) {
		try {
			Files.write(path, new DotCall(this.dot)
				                  .layoutEngine(this.dotEngine)
				                  .outputFormat(format)
				                  .input(this.currentPumlContent.get())
				                  .call());
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to save file converted from dot", e);
		}
	}*/

	@FXML
	private void defaultSize() {
		pumlView.setZoom(1);
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
		pumlView.setZoom(pumlView.getZoom() * factor);
	}

	private void adjustScroll() {
		Set<Node> nodes = pumlView.lookupAll(".scroll-bar");
		double x = 0.0;
		double y = 0.0;
		for (final Node node : nodes) {
			if (node instanceof ScrollBar sb) {
				if (sb.getOrientation() == Orientation.VERTICAL) {
					x = sb.getPrefHeight() / 2;
				} else {
					y = sb.getPrefWidth() / 2;
				}
			}
		}
		pumlView.getEngine().executeScript("window.scrollBy(" + x + "," + y + ")");
	}

	@Override
	protected void clearContent() {
		this.currentPumlContent.set(null);
		pumlView.getEngine().loadContent("");
		pumlView.setVisible(false);
		placeholderLabel.setVisible(true);
	}

	@Override
	protected PlantUmlFormulaTask createNewTask(String id, String command, String formula) {
		return new PlantUmlFormulaTask(id, command, formula);
	}
}
