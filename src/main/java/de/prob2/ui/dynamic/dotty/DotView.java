package de.prob2.ui.dynamic.dotty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetDotForVisualizationCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.parser.BindingGenerator;
import de.prob.prolog.term.PrologTerm;
import de.prob.statespace.Trace;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
public class DotView extends DynamicCommandStage {

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

	private final StringProperty currentSvg;

	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	

	@Inject
	public DotView(final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle, final Injector injector) {
		super(stageManager, preferences, currentTrace, currentProject, bundle, injector);
		this.currentSvg = new SimpleStringProperty(this, "currentSvg", null);
		stageManager.loadFXML(this, "dot_view.fxml");
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();
		stageManager.setMacMenuBar(this, this.menuBar);
		saveButton.disableProperty().bind(currentSvg.isNull());
		helpButton.setHelpContent(this.getClass());
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
		
	}

	@Override
	protected void fillCommands() {
		super.fillCommands(new GetAllDotCommands(currentTrace.getCurrentState()));
	}

	@Override
	protected void visualize(DynamicCommandItem item) {
		if (!item.isAvailable()) {
			return;
		}
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		interrupt();

		Thread thread = new Thread(() -> {
			Platform.runLater(()-> statusBar.setText(bundle.getString("statusbar.loadStatus.loading")));
			try {
				Trace trace = currentTrace.get();
				if(trace == null || (item.getArity() > 0 && taFormula.getText().isEmpty())) {
					Platform.runLater(this::reset);
					currentThread.set(null);
					return;
				}
				final String text = getSvgForDotCommand(trace, item, formulas);
				if(!Thread.currentThread().isInterrupted()) {
					loadGraph(text);
				}
			} catch (InterruptedException e) {
				LOGGER.info("Dot visualization interrupted", e);
				Thread.currentThread().interrupt();
				Platform.runLater(this::reset);
			} catch (IOException | UncheckedIOException | ProBError | EvaluationException e) {
				LOGGER.error("Graph visualization failed", e);
				currentThread.set(null);
				Platform.runLater(() -> {
					taErrors.setText(e.getMessage());
					dotView.getEngine().loadContent("");
					statusBar.setText("");
				});
			}
		}, "Graph Visualizer");
		currentThread.set(thread);
		thread.start();
	}

	private String getSvgForDotCommand(final Trace trace, final DynamicCommandItem item, final List<IEvalElement> formulas) throws IOException, InterruptedException {
		if (item.getArity() > 0) {
			formulas.add(trace.getModel().parseFormula(taFormula.getText(), FormulaExpand.EXPAND));
		}
		final Path dotFilePath = Files.createTempFile("prob2-ui", ".dot");
		
		try {
			final GetPreferenceCommand getDotCmd = new GetPreferenceCommand("DOT");
			final GetPreferenceCommand getDotEngineCmd = new GetPreferenceCommand("DOT_ENGINE");
			final ComposedCommand ccmd = new ComposedCommand(
				getDotCmd,
				getDotEngineCmd,
				new GetDotForVisualizationCommand(trace.getCurrentState(), item, dotFilePath.toFile(), formulas)
			);
			trace.getStateSpace().execute(ccmd);
			if (ccmd.isInterrupted()) {
				throw new InterruptedException("Visualization command execution was interrupted");
			}
			final String dot = getDotCmd.getValue();
			final String dotEngine = item.getAdditionalInfo().stream()
				.filter(t -> "preferred_dot_type".equals(t.getFunctor()))
				.map(t -> BindingGenerator.getCompoundTerm(t, 1))
				.map(t -> PrologTerm.atomicString(t.getArgument(1)))
				.findAny()
				.orElseGet(getDotEngineCmd::getValue);
			return getSvgForDotFile(dot, dotEngine, dotFilePath);
		} finally {
			try {
				Files.delete(dotFilePath);
			} catch (IOException e) {
				LOGGER.error("Failed to delete temporary dot file", e);
			}
		}
	}

	private static String getSvgForDotFile(final String dotCommand, final String dotEngine, final Path dotFilePath) throws IOException, InterruptedException {
		final ProcessBuilder dotProcessBuilder = new ProcessBuilder(dotCommand, "-K" + dotEngine, "-Tsvg", dotFilePath.toString());
		LOGGER.debug("Starting dot command: {}", dotProcessBuilder.command());
		final Process dotProcess = dotProcessBuilder.start();
		
		// Read stderr in a background thread, to prevent the stream buffer from filling up and blocking dot.
		// (This is very unlikely to happen, because dot normally doesn't produce a lot of stderr output.)
		final StringJoiner errorOutput = new StringJoiner("\n");
		final Thread stderrLogger = new Thread(() -> {
			try (
				final Reader reader = new InputStreamReader(dotProcess.getErrorStream());
				final BufferedReader br = new BufferedReader(reader);
			) {
				br.lines().forEach(line -> {
					errorOutput.add(line);
					LOGGER.error("Error output from dot: {}", line);
				});
			} catch (IOException e) {
				LOGGER.error("Failed to read dot error output", e);
			}
		}, "dot stderr logger");
		stderrLogger.start();
		
		// Read stdout while dot is running, to prevent the stream buffer from filling up and blocking dot.
		// (Unlike with stderr, this actually happens in practice, when the generated SVG is large.)
		final String svg;
		try (
			final Reader reader = new InputStreamReader(dotProcess.getInputStream());
			final BufferedReader br = new BufferedReader(reader);
		) {
			svg = br.lines().collect(Collectors.joining("\n"));
		}
		
		final int exitCode = dotProcess.waitFor();
		LOGGER.debug("dot exited with status code {}", exitCode);
		
		if (exitCode != 0) {
			stderrLogger.join(); // Make sure that all stderr output has been read
			throw new ProBError("dot exited with status code " + exitCode + ":\n" + errorOutput);
		}
		
		return svg;
	}

	private void loadGraph(final String svg) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			this.currentSvg.set(svg);
			if (!thread.isInterrupted()) {
				dotView.getEngine().loadContent("<center>" + svg + "</center>");
				statusBar.setText("");
				taErrors.clear();
			}
			currentThread.set(null);
		});
	}
	
	@FXML
	private void save() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.svg"), "*.svg"));
		fileChooser.setTitle(bundle.getString("common.fileChooser.save.title"));
		final File file = fileChooser.showSaveDialog(this.getScene().getWindow());
		if (file == null) {
			return;
		}
		try {
			Files.write(file.toPath(), currentSvg.get().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOGGER.error("Failed to save SVG", e);
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
		currentSvg.set(null);
		dotView.getEngine().loadContent("");
		statusBar.setText("");
	}
	
	@FXML
	private void editPreferences() {
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}

	public void visualizeFormula(final Object formula) {
		taErrors.clear();
		Thread thread = new Thread(() -> {
			try {
				DynamicCommandItem choice = lvChoice.getItems().stream()
						.filter(item -> "formula_tree".equals(item.getCommand()))
						.collect(Collectors.toList())
						.get(0);
				Platform.runLater(() -> {
					statusBar.setText(bundle.getString("statusbar.loadStatus.loading"));
					if(formula instanceof IEvalElement) {
						taFormula.setText(((IEvalElement) formula).getCode());
					} else {
						taFormula.setText((String) formula);
					}
					lvChoice.getSelectionModel().select(choice);
					visualize(choice);
				});
			} catch (EvaluationException | ProBError exception) {
				Platform.runLater(() -> taErrors.setText(exception.getMessage()));
			}
		});
		currentThread.set(thread);
		thread.start();
	}

}
