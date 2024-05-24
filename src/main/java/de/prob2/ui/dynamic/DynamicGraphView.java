package de.prob2.ui.dynamic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.DotCall;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.PlantUmlVisualizationCommand;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.plantuml.JavaLocator;
import de.prob2.ui.dynamic.plantuml.PlantUmlCall;
import de.prob2.ui.dynamic.plantuml.PlantUmlLocator;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class DynamicGraphView extends BorderPane implements Builder<DynamicGraphView> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicGraphView.class);

	@FXML
	private WebView webView;
	@FXML
	private Button saveButton;

	private final StageManager stageManager;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final JavaLocator javaLocator;
	private final PlantUmlLocator plantUmlLocator;
	private final ObjectProperty<GraphMode> graphMode;
	private final ObjectProperty<byte[]> currentInput;
	private final StringProperty currentSvg;

	private String cachedDotCommand;
	private String cachedDotEngine;

	@Inject
	public DynamicGraphView(StageManager stageManager, I18n i18n, FileChooserManager fileChooserManager, JavaLocator javaLocator, PlantUmlLocator plantUmlLocator) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.javaLocator = javaLocator;
		this.plantUmlLocator = plantUmlLocator;
		this.graphMode = new SimpleObjectProperty<>(this, "graphMode", null);
		this.currentInput = new SimpleObjectProperty<>(this, "currentInput", null);
		this.currentSvg = new SimpleStringProperty(this, "currentSvg", null);
		stageManager.loadFXML(this, "graph_view.fxml");
	}

	@FXML
	private void initialize() {
		this.currentSvg.addListener((observable, from, to) -> this.loadGraph(to));
		this.saveButton.disableProperty().bind(this.graphMode.isNull());

		this.webView.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
			Set<Node> scrollBars = this.webView.lookupAll(".scroll-bar");
			for (Node scrollBar : scrollBars) {
				scrollBar.setStyle("-fx-opacity: 0.5;");
			}
		});

		// TODO: zoom
	}

	@Override
	public DynamicGraphView build() {
		return this;
	}

	@FXML
	private void save() {
		if (this.graphMode.get() == null || this.currentInput.get() == null || this.currentSvg.get() == null) {
			return;
		}

		FileChooser fileChooser = new FileChooser();

		FileChooser.ExtensionFilter svgFilter = this.fileChooserManager.getSvgFilter();
		FileChooser.ExtensionFilter pngFilter = this.fileChooserManager.getPngFilter();
		FileChooser.ExtensionFilter pdfFilter = this.fileChooserManager.getPdfFilter();
		FileChooser.ExtensionFilter inputFilter;
		switch (this.graphMode.get()) {
			case DOT -> {
				inputFilter = this.fileChooserManager.getDotFilter();
				fileChooser.getExtensionFilters().setAll(inputFilter, svgFilter, pngFilter, pdfFilter);
			}
			case PUML -> {
				inputFilter = this.fileChooserManager.getPumlFilter();
				fileChooser.getExtensionFilters().setAll(inputFilter, svgFilter, pngFilter);
			}
			default -> throw new AssertionError();
		}

		fileChooser.setTitle(this.i18n.translate("common.fileChooser.save.title"));
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this.getScene().getWindow());
		if (path == null || path.getFileName() == null || path.getParent() == null) {
			return;
		}

		FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
		try {
			Files.createDirectories(path.getParent());
			if (inputFilter.equals(selectedFilter)) {
				Files.write(path, this.currentInput.get());
			} else if (svgFilter.equals(selectedFilter)) {
				Files.writeString(path, this.currentSvg.get(), StandardCharsets.UTF_8);
			} else {
				switch (this.graphMode.get()) {
					case DOT -> {
						String outputFormat;
						if (pngFilter.equals(selectedFilter)) {
							outputFormat = DotOutputFormat.PNG;
						} else if (pdfFilter.equals(selectedFilter)) {
							outputFormat = DotOutputFormat.PDF;
						} else {
							throw new AssertionError();
						}

						byte[] data = this.visualizeDot(this.cachedDotCommand, this.cachedDotEngine, outputFormat, this.currentInput.get());
						if (data != null) {
							Files.write(path, data);
						}
					}
					case PUML -> {
						String outputFormat;
						if (pngFilter.equals(selectedFilter)) {
							outputFormat = PlantUmlCall.PNG;
						} else {
							throw new AssertionError();
						}

						byte[] data = this.visualizePuml(this.javaLocator.getJavaExecutable(), this.plantUmlLocator.findPlantUmlJar().orElseThrow(), outputFormat, this.currentInput.get());
						if (data != null) {
							Files.write(path, data);
						}
					}
					default -> throw new AssertionError();
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to save graph", e);
			Alert alert = this.stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path);
			alert.initOwner(this.getScene().getWindow());
			alert.showAndWait();
		}
	}

	void clearContent() {
		this.setVisible(false);
		this.cachedDotCommand = null;
		this.cachedDotEngine = null;
		this.currentInput.set(null);
		this.currentSvg.set(null);
	}

	void visualize(DotVisualizationCommand command, List<IEvalElement> formulas) throws InterruptedException {
		String dotCommand = command.getTrace().getStateSpace().getCurrentPreference("DOT");
		String dotEngine = command.getPreferredDotLayoutEngine().orElseGet(() -> command.getTrace().getStateSpace().getCurrentPreference("DOT_ENGINE"));

		// Make sure dot and engine are set, else react with proper error message
		if (dotCommand == null || dotCommand.isEmpty()) {
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "dotty.error.emptyDotPath.header", "dotty.error.emptyDotPath.message");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			});
			return;
		} else if (dotEngine == null || dotEngine.isEmpty()) {
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "dotty.error.emptyDotEngine.header", "dotty.error.emptyDotEngine.message");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			});
			return;
		}

		byte[] dotInput = command.visualizeAsDotToBytes(formulas);
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		String outputFormat = DotOutputFormat.SVG;
		byte[] svgBytes = this.visualizeDot(dotCommand, dotEngine, outputFormat, dotInput);
		if (svgBytes == null) {
			return;
		}

		String svgString = new String(svgBytes, StandardCharsets.UTF_8);
		if (!Thread.currentThread().isInterrupted()) {
			Platform.runLater(() -> {
				this.cachedDotCommand = dotCommand;
				this.cachedDotEngine = dotEngine;
				this.graphMode.set(GraphMode.DOT);
				this.currentInput.set(dotInput);
				this.currentSvg.set(svgString);
				this.setVisible(true);
			});
		}
	}

	void visualize(PlantUmlVisualizationCommand command, List<IEvalElement> formulas) throws InterruptedException {
		String javaExecutable = this.javaLocator.getJavaExecutable();
		Optional<Path> optPlantUmlJar = this.plantUmlLocator.findPlantUmlJar();
		if (optPlantUmlJar.isEmpty()) {
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeAlert(
						Alert.AlertType.ERROR,
						"plantuml.error.noPlantUml.header",
						"plantuml.error.noPlantUml.message",
						"https://plantuml.com/download",
						this.plantUmlLocator.getDirectory()
				);
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			});
			return;
		}
		Path plantUmlJar = optPlantUmlJar.get();

		byte[] pumlInput = command.visualizeAsPlantUmlToBytes(formulas);
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		String outputFormat = PlantUmlCall.SVG;
		byte[] svgBytes = this.visualizePuml(javaExecutable, plantUmlJar, outputFormat, pumlInput);
		if (svgBytes == null) {
			return;
		}

		String svgString = new String(svgBytes, StandardCharsets.UTF_8);
		if (!Thread.currentThread().isInterrupted()) {
			Platform.runLater(() -> {
				this.cachedDotCommand = null;
				this.cachedDotEngine = null;
				this.graphMode.set(GraphMode.PUML);
				this.currentInput.set(pumlInput);
				this.currentSvg.set(svgString);
				this.setVisible(true);
			});
		}
	}

	private byte[] visualizeDot(String dotCommand, String dotEngine, String outputFormat, byte[] dotInput) throws InterruptedException {
		try {
			return new DotCall(dotCommand)
					.layoutEngine(dotEngine)
					.outputFormat(outputFormat)
					.input(dotInput)
					.call();
		} catch (Exception e) {
			LOGGER.error("could not visualize graph with dot (command={}, layoutEngine={}, outputFormat={})", dotCommand, dotEngine, outputFormat, e);
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeExceptionAlert(e, "dotty.error.dotVisualization.header", "dotty.error.dotVisualization.message", dotCommand, dotEngine, outputFormat);
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			});
			return null;
		}
	}

	private byte[] visualizePuml(String javaCommand, Path plantUmlJar, String outputFormat, byte[] pumlInput) throws InterruptedException {
		try {
			return new PlantUmlCall(javaCommand, plantUmlJar)
					.outputFormat(outputFormat)
					.input(pumlInput)
					.call();
		} catch (Exception e) {
			LOGGER.error("could not visualize graph with plantuml (java={}, plantuml={}, outputFormat={})", javaCommand, plantUmlJar, outputFormat, e);
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeExceptionAlert(e, "plantuml.error.plantumlVisualization.header", "plantuml.error.plantumlVisualization.message", javaCommand, plantUmlJar, outputFormat);
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			});
			return null;
		}
	}

	private void loadGraph(String svg) {
		if (svg != null) {
			this.webView.getEngine().loadContent("<center>" + svg + "</center>");
		} else {
			this.webView.getEngine().loadContent("");
		}
	}

	private enum GraphMode {
		DOT, PUML
	}
}
