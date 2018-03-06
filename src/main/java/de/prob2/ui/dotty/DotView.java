package de.prob2.ui.dotty;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DotView extends Stage {

	private final class DotCommandCell extends ListCell<DynamicCommandItem> {

		public DotCommandCell() {
			super();
			getStyleClass().add("dot-command-cell");
		}

		@Override
		protected void updateItem(DynamicCommandItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("dotcommandenabled", "dotcommanddisabled"));
			if (item != null && !empty) {
				setText(item.getName());
				if (item.isAvailable()) {
					getStyleClass().add("dotcommandenabled");
				} else {
					getStyleClass().add("dotcommanddisabled");
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);

	private static final File FILE = new File(
			Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "out.svg");

	@FXML
	private WebView dotView;

	@FXML
	private ListView<DynamicCommandItem> lvChoice;

	@FXML
	private TextArea taFormula;

	@FXML
	private VBox enterFormulaBox;

	@FXML
	private Label lbDescription;

	@FXML
	private Label lbAvailable;

	@FXML
	private CheckBox cbContinuous;

	@FXML
	private ScrollPane pane;

	private DynamicCommandItem currentItem;

	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;

	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;

	private Thread currentThread;

	private Thread loadedThread;

	private boolean loaded;

	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace,
			final CurrentProject currentProject, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.bundle = bundle;
		stageManager.loadFXML(this, "dot_view.fxml");
	}

	@FXML
	public void initialize() {
		initializeZooming();
		lvChoice.getSelectionModel().selectFirst();
		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			if (!to.isAvailable()) {
				lbAvailable.setText(String.join("\n", bundle.getString("dotview.notavailable"), to.getAvailable()));
			} else {
				lbAvailable.setText("");
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			lbDescription.setText(to.getDescription());
			String currentFormula = taFormula.getText();
			if ((!needFormula || !currentFormula.isEmpty()) && (currentItem == null
					|| !currentItem.getCommand().equals(to.getCommand()) || cbContinuous.isSelected())) {
				dotView.getEngine().loadContent("");
				visualize(to);
				currentItem = to;
			}
		});
		fillCommands();
		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			int index = lvChoice.getSelectionModel().getSelectedIndex();
			fillCommands();
			if (index == -1) {
				return;
			}
			lvChoice.getSelectionModel().select(index);
		});

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			fillCommands();
			dotView.getEngine().loadContent("");
		});

		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
					if (item == null) {
						return;
					}
					visualize(item);
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			}
		});
		lvChoice.setCellFactory(item -> new DotCommandCell());
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

	private void fillCommands() {
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

	private void visualize(DynamicCommandItem item) {
		if (!item.isAvailable()) {
			return;
		}
		ArrayList<IEvalElement> formulas = new ArrayList<>();
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

	@FXML
	private void handleClose() {
		this.close();
	}

	private void interrupt() {
		if (currentThread != null) {
			currentThread.interrupt();
		}
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

}
