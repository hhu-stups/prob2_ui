package de.prob2.ui.dynamic.dotty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.DotCall;
import de.prob.animator.domainobjects.DotOutputFormat;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
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
import javafx.scene.Cursor;
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

	private final FileChooserManager fileChooserManager;

	private String dot;
	private String dotEngine;
	private final ObjectProperty<byte[]> currentDotContent;

	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	

	@Inject
	public DotView(final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager, final StopActions stopActions) {
		super(stageManager, preferences, currentTrace, currentProject, bundle, stopActions, "Graph Visualizer");
		
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
		dotView.setOnMouseMoved(e -> {
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});

		dotView.setOnMouseDragged(e -> {
			pane.setHvalue(pane.getHvalue() + (-e.getSceneX() + oldMousePositionX) / (pane.getWidth() * dragFactor));
			pane.setVvalue(pane.getVvalue() + (-e.getSceneY() + oldMousePositionY) / (pane.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});

		dotView.setOnMouseMoved(e -> dotView.setCursor(Cursor.HAND));
		dotView.setOnMouseDragged(e -> dotView.setCursor(Cursor.MOVE));

		dotView.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(Change<? extends Node> c) {
				Set<Node> scrollBars = dotView.lookupAll(".scroll-bar");
				for (Node scrollBar : scrollBars) {
					scrollBar.setStyle("-fx-opacity: 0.5;");
				}
			}
		});

	}

	@Override
	protected List<DotVisualizationCommand> getCommandsInState(final State state) {
		return DotVisualizationCommand.getAll(state);
	}

	@Override
	protected void visualize(DotVisualizationCommand item) {
		if (!item.isAvailable()) {
			return;
		}
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		interrupt();

		this.updater.execute(() -> {
			Platform.runLater(()-> statusBar.setText(bundle.getString("statusbar.loadStatus.loading")));
			try {
				Trace trace = currentTrace.get();
				if(trace == null || (item.getArity() > 0 && taFormula.getText().isEmpty())) {
					Platform.runLater(this::reset);
					return;
				}
				setUpSvgForDotCommand(trace, item, formulas);
				if(!Thread.currentThread().isInterrupted()) {
					final byte[] svgData = new DotCall(this.dot)
						.layoutEngine(this.dotEngine)
						.outputFormat(DotOutputFormat.SVG)
						.input(this.currentDotContent.get())
						.call();
					loadGraph(new String(svgData, StandardCharsets.UTF_8));
				}
			} catch (CommandInterruptedException | InterruptedException e) {
				LOGGER.info("Dot visualization interrupted", e);
				Thread.currentThread().interrupt();
				Platform.runLater(this::reset);
			} catch (IOException | UncheckedIOException | ProBError | EvaluationException e) {
				LOGGER.error("Graph visualization failed", e);
				Platform.runLater(() -> {
					taErrors.setText(e.getMessage());
					dotView.getEngine().loadContent("");
					statusBar.setText("");
				});
			}
		});
	}

	private void setUpSvgForDotCommand(final Trace trace, final DotVisualizationCommand item, final List<IEvalElement> formulas) throws IOException, InterruptedException {
		if (item.getArity() > 0) {
			formulas.add(trace.getModel().parseFormula(taFormula.getText(), FormulaExpand.EXPAND));
		}
		
		this.dot = trace.getStateSpace().getCurrentPreference("DOT");
		this.dotEngine = item.getPreferredDotLayoutEngine()
			.orElseGet(() -> trace.getStateSpace().getCurrentPreference("DOT_ENGINE"));
		this.currentDotContent.set(item.visualizeAsDotToBytes(formulas));
	}

	private void loadGraph(final String svgContent) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			if (!thread.isInterrupted()) {
				dotView.getEngine().loadContent("<center>" + svgContent + "</center>");
				statusBar.setText("");
				taErrors.clear();
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
		dragFactor *= factor;
	}

	private void adjustScroll() {
		Set<Node> nodes = pane.lookupAll(".scroll-bar");
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
	protected void reset() {
		this.dot = null;
		this.dotEngine = null;
		this.currentDotContent.set(null);
		dotView.getEngine().loadContent("");
		statusBar.setText("");
	}
	
	@FXML
	private void editPreferences() {
		DotVisualizationCommand currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}

	public void visualizeFormula(final String formula) {
		taErrors.clear();
		try {
			DotVisualizationCommand choice = lvChoice.getItems().stream()
					.filter(item -> "formula_tree".equals(item.getCommand()))
					.collect(Collectors.toList())
					.get(0);
			statusBar.setText(bundle.getString("statusbar.loadStatus.loading"));
			taFormula.setText(formula);
			lvChoice.getSelectionModel().select(choice);
			visualize(choice);
		} catch (EvaluationException | ProBError exception) {
			taErrors.setText(exception.getMessage());
		}
	}

}
