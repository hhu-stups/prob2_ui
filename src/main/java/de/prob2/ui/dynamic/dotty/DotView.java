package de.prob2.ui.dynamic.dotty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.DotCall;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DotView extends DynamicCommandStage<DotVisualizationCommand> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);

	@FXML
	private WebView dotView;

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

	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;

	private String dot;
	private String dotEngine;
	private final ObjectProperty<byte[]> currentDotContent;

	@Inject
	public DotView(final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager, final StopActions stopActions) {
		super(preferences, currentTrace, currentProject, bundle, stopActions, "Graph Visualizer");
		
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		
		this.dot = null;
		this.dotEngine = null;
		this.currentDotContent = new SimpleObjectProperty<>(this, "currentDotContent", null);
		stageManager.loadFXML(this, "dot_view.fxml");
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();
		stageManager.setMacMenuBar(this, this.menuBar);
		saveButton.disableProperty().bind(currentDotContent.isNull());
		helpButton.setHelpContent("graphVisualisation", null);
		initializeZooming();
	}

	private void initializeZooming() {
		dotView.getChildrenUnmodifiable().addListener((ListChangeListener<Node>)c -> {
			Set<Node> scrollBars = dotView.lookupAll(".scroll-bar");
			for (Node scrollBar : scrollBars) {
				scrollBar.setStyle("-fx-opacity: 0.5;");
			}
		});

	}

	@Override
	protected List<DotVisualizationCommand> getCommandsInState(final State state) {
		return DotVisualizationCommand.getAll(state);
	}

	@Override
	protected void visualizeInternal(final DotVisualizationCommand item, final List<IEvalElement> formulas) throws InterruptedException {
		this.dot = item.getState().getStateSpace().getCurrentPreference("DOT");
		this.dotEngine = item.getPreferredDotLayoutEngine()
			.orElseGet(() -> item.getState().getStateSpace().getCurrentPreference("DOT_ENGINE"));
		this.currentDotContent.set(item.visualizeAsDotToBytes(formulas));
		if(!Thread.currentThread().isInterrupted()) {
			final byte[] svgData = new DotCall(this.dot)
				.layoutEngine(this.dotEngine)
				.outputFormat(DotOutputFormat.SVG)
				.input(this.currentDotContent.get())
				.call();
			loadGraph(new String(svgData, StandardCharsets.UTF_8));
		}
	}

	private void loadGraph(final String svgContent) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			if (!thread.isInterrupted()) {
				dotView.getEngine().loadContent("<center>" + svgContent + "</center>");
				this.clearLoadingStatus();
				taErrors.clear();
				placeholderLabel.setVisible(false);
				dotView.setVisible(true);
			}
		});
	}
	
	@FXML
	private void save() {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter svgFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.svg", "svg");
		FileChooser.ExtensionFilter pngFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.png", "png");
		FileChooser.ExtensionFilter dotFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.dot", "dot");
		FileChooser.ExtensionFilter pdfFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.pdf", "pdf");
		fileChooser.getExtensionFilters().setAll(svgFilter, pngFilter, dotFilter, pdfFilter);
		fileChooser.setTitle(bundle.getString("common.fileChooser.save.title"));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, this.getScene().getWindow());
		if (path == null) {
			return;
		}
		FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
		if(selectedFilter.equals(dotFilter)) {
			saveDot(path);
		} else {
			final String format = getTargetFormat(selectedFilter, svgFilter, pngFilter, pdfFilter);
			saveConverted(format, path);
		}
	}

	private String getTargetFormat(FileChooser.ExtensionFilter selectedFilter, FileChooser.ExtensionFilter svgFilter, FileChooser.ExtensionFilter pngFilter, FileChooser.ExtensionFilter pdfFilter) {
		if(selectedFilter.equals(svgFilter)) {
			return DotOutputFormat.SVG;
		} else if(selectedFilter.equals(pngFilter)) {
			return DotOutputFormat.PNG;
		} else if(selectedFilter.equals(pdfFilter)) {
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
	
	@Override
	protected void clearContent() {
		this.dot = null;
		this.dotEngine = null;
		this.currentDotContent.set(null);
		dotView.getEngine().loadContent("");
		dotView.setVisible(false);
		placeholderLabel.setVisible(true);
	}
	
	@FXML
	private void editPreferences() {
		DotVisualizationCommand currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}

	public void visualizeFormula(final String formula) {
		this.selectCommand(DotVisualizationCommand.FORMULA_TREE_NAME, formula);
	}

}
