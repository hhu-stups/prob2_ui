package de.prob2.ui.dotty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DynamicCommandStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DotView extends DynamicCommandStage {

	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);

	@FXML
	private WebView dotView;
	
	@FXML
	private Button zoomOutButton;
	
	@FXML
	private Button zoomInButton;
	
	@FXML
	private HBox zoomBox;
	
	@FXML
	private HelpButton helpButton;
	

	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	

	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle, final Injector injector) {
		super(stageManager, currentTrace, currentProject, bundle, injector);
		stageManager.loadFXML(this, "dot_view.fxml");
	}

	@FXML
	@Override
	public void initialize() {
		super.initialize();
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
				if (item.getArity() > 0) {
					formulas.add(new ClassicalB(taFormula.getText(), FormulaExpand.EXPAND));
				}
				State id = currentTrace.getCurrentState();
				final Path path = Files.createTempFile("prob2-ui-dot", ".svg");
				GetSvgForVisualizationCommand cmd = new GetSvgForVisualizationCommand(id, item, path.toFile(), formulas);
				currentTrace.getStateSpace().execute(cmd);
				final String text;
				try (final Stream<String> lines = Files.lines(path)) {
					text = lines.collect(Collectors.joining("\n"));
				}
				Files.delete(path);
				if(!Thread.currentThread().isInterrupted()) {
					loadGraph(text);
				}
			} catch (IOException | ProBError | EvaluationException e) {
				LOGGER.error("Graph visualization failed", e);
				currentThread.set(null);
				Platform.runLater(() -> {
					stageManager.makeExceptionAlert(e, "dotty.alerts.visualisationError.content").show();
					dotView.getEngine().loadContent("");
					statusBar.setText("");
				});
			}
		}, "Graph Visualizer");
		currentThread.set(thread);
		thread.start();
	}

	private void loadGraph(final String svg) {
		Thread thread = Thread.currentThread();
		Platform.runLater(() -> {
			if (!thread.isInterrupted()) {
				/*
				 * FIXME: Fix rendering problem in JavaFX WebView
				 */
				dotView.getEngine().loadContent("<center>" + svg.replaceAll("font-size=\"12.00\"", "font-size=\"10.00\"") + "</center>");
				statusBar.setText("");
			}
			currentThread.set(null);
		});
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
		dotView.getEngine().loadContent("");
		statusBar.setText("");
	}

}
