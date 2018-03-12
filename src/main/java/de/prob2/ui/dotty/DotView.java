package de.prob2.ui.dotty;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.internal.DynamicCommandItemCell;
import de.prob2.ui.internal.DynamicCommandStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.web.WebView;

public class DotView extends DynamicCommandStage {

	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);

	private static final File FILE = new File(
			Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "out.svg");

	@FXML
	private WebView dotView;

	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	
	private Thread loadedThread;

	private boolean loaded;

	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle) {
		super(stageManager, currentTrace, currentProject, bundle);
		stageManager.loadFXML(this, "dot_view.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		initializeZooming();
		lvChoice.setCellFactory(item -> new DynamicCommandItemCell("dot-command-cell","dotcommandenabled", "dotcommanddisabled"));
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
	}

	@Override
	protected void fillCommands() {
		try {
			lvChoice.getItems().clear();
			State id = currentTrace.getCurrentState();
			GetAllDotCommands cmd = new GetAllDotCommands(id);
			currentTrace.getStateSpace().execute(cmd);
			for (DynamicCommandItem item : cmd.getCommands()) {
				lvChoice.getItems().add(item);
			}
		} catch (Exception e) {
			LOGGER.error("Extract all dot commands failed", e);
		}
	}

	@Override
	protected void visualize(DynamicCommandItem item) {
		if (!item.isAvailable()) {
			return;
		}
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		interrupt();
		loaded = false;

		currentThread = new Thread(() -> {
			try {
				if (item.getArity() > 0) {
					formulas.add(new ClassicalB(taFormula.getText()));
				}
				State id = currentTrace.getCurrentState();
				GetSvgForVisualizationCommand cmd = new GetSvgForVisualizationCommand(id, item, FILE, formulas);
				currentTrace.getStateSpace().execute(cmd);
				loadGraph();
			} catch (ProBError | EvaluationException e) {
				LOGGER.error("Graph visualization failed", e);
				Platform.runLater(() -> {
					stageManager.makeExceptionAlert(bundle.getString("dotview.error.message"), e).show();
					dotView.getEngine().loadContent("");
				});
			}
		}, "Graph Visualizer");
		currentThread.start();

		loadedThread = new Thread(() -> {
			try {
				Thread.sleep(500);
				if (!loaded) {
					Platform.runLater(() -> dotView.getEngine()
							.loadContent("<center><h1>" + bundle.getString("dotview.loading") + "</h1></center>"));
				}
			} catch (InterruptedException e) {
				LOGGER.debug("DotView loading interrupted (this is not an error)", e);
				Thread.currentThread().interrupt();
			}
		});
		loadedThread.start();
	}

	private void loadGraph() {
		Platform.runLater(() -> {
			String content = "";
			try {
				/*
				 * FIXME: Fix rendering problem in JavaFX WebView
				 */
				content = new String(Files.readAllBytes(FILE.toPath())).replaceAll("font-size=\"12.00\"",
						"font-size=\"10.00\"");
			} catch (Exception e) {
				LOGGER.error("Reading dot file failed", e);
				return;
			}
			dotView.getEngine().loadContent("<center>" + content + "</center>");
			loaded = true;
		});
	}

	@FXML
	private void cancel() {
		interrupt();
		if (!loaded) {
			dotView.getEngine().loadContent("");
		}
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

	@Override
	protected void interrupt() {
		super.interrupt();
		if (loadedThread != null) {
			loadedThread.interrupt();
		}
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
	}

}
