package de.prob2.ui.dotty;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DotCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DotView extends Stage {
	
	private final class DotCommandCell extends ListCell<DotCommandItem> {
		
		public DotCommandCell() {
			super();
			getStyleClass().add("dot-command-cell");
		}
		
		@Override
		protected void updateItem(DotCommandItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("dotcommandenabled", "dotcommanddisabled"));
			if(item != null && !empty) {
				setText(item.getName());
				if(item.isAvailable()) {
					getStyleClass().add("dotcommandenabled");
				} else {
					getStyleClass().add("dotcommanddisabled");
				}
			}
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);
	
	private static final File FILE = new File(Main.getProBDirectory()
			+ File.separator + "prob2ui" + File.separator + "out.svg");
	
	@FXML
	private WebView dotView;
	
	@FXML
	private ListView<DotCommandItem> lvChoice;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private HBox enterFormulaBox;
	
	@FXML
	private Label lbDescription;
	
	@FXML
	private Label lbAvailable;
	
	@FXML
	private CheckBox cbContinuous;
	
	@FXML
	private ScrollPane pane;
	
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	private boolean stateChanged = false;
	
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;
	
	private Thread currentThread;
	
	
	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle) {
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
			if(to == null) {
				return;
			}
			if(!to.isAvailable()) {
				lbAvailable.setText(String.join("\n", bundle.getString("dotview.notavailable"), to.getAvailable()));
			} else {
				lbAvailable.setText("");
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			lbDescription.setText(to.getDescription());
			if(!needFormula && (!stateChanged || cbContinuous.isSelected())) {
				dotView.getEngine().loadContent("");
				visualize(to);
			}
			stateChanged = false;
		});
		fillCommands();
		currentTrace.currentStateProperty().addListener((observable, from, to) ->  {
			int index = lvChoice.getSelectionModel().getSelectedIndex();
			fillCommands();
			if(index == -1) {
				return;
			}
			lvChoice.getSelectionModel().select(index);
			stateChanged = true;
		});
		
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			fillCommands();
			dotView.getEngine().loadContent("");
		});
		
		tfFormula.setOnKeyPressed(e -> {
			if(e.getCode().equals(KeyCode.ENTER)) {
				DotCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
				if(item == null) {
					return;
				}
				visualize(item);
			}
		});
		
		lvChoice.setCellFactory(item -> new DotCommandCell());
	}
	
	private void initializeZooming() {
		dotView.setOnMouseMoved(e-> {
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		dotView.setOnMouseDragged(e-> {
			pane.setHvalue(pane.getHvalue() + (-e.getSceneX() + oldMousePositionX)/(pane.getWidth() * dragFactor));
			pane.setVvalue(pane.getVvalue() + (-e.getSceneY() + oldMousePositionY)/(pane.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});

		dotView.setOnMouseClicked(e-> {
			if(e.getClickCount() < 2) {
				return;
			}
			
			if(e.getButton() == MouseButton.SECONDARY) {
				dotView.setZoom(dotView.getZoom() * 0.9);
				dragFactor *= 0.9;
			} else {
				dotView.setZoom(dotView.getZoom() * 1.1);
				dragFactor *= 1.1;
			}
			
			double x = e.getX()/(2*dragFactor);
			double y = e.getY()/(2*dragFactor);
			dotView.getEngine().executeScript("window.scrollBy(" + x + "," + y +")");
		});
	}
	
	private void fillCommands() {
		try {
			lvChoice.getItems().clear();
			State id = currentTrace.getCurrentState();
			GetAllDotCommands cmd = new GetAllDotCommands(id);
			currentTrace.getStateSpace().execute(cmd);
			for(DotCommandItem item : cmd.getCommands()) {
				lvChoice.getItems().add(item);
			}
		} catch (Exception e) {
			LOGGER.error("Extract all dot commands failed", e);
		}
	}
	
	private void visualize(DotCommandItem item) {
		if(!item.isAvailable()) {
			return;
		}
		ArrayList<IEvalElement> formulas = new ArrayList<>();
		if(currentThread != null) {
			currentThread.interrupt();
		}
		currentThread = new Thread(() -> {
			try {
				if(item.getArity() > 0) {
					formulas.add(new ClassicalB(tfFormula.getText()));
				}
				State id = currentTrace.getCurrentState();
				GetSvgForVisualizationCommand cmd = new GetSvgForVisualizationCommand(id, item, FILE, formulas);
				currentTrace.getStateSpace().execute(cmd);
				loadGraph();
			} catch (IOException | ProBError | EvaluationException e) {
				LOGGER.error("Graph visualization failed", e);
				Platform.runLater(() -> stageManager.makeExceptionAlert(bundle.getString("dotview.error.message"), e).show());
			}
		}, "Graph Visualizer");
		currentThread.start();
	}
	
	private void loadGraph() throws IOException {
		Platform.runLater(() -> {
			String content = "";
			try {
				content = new String(Files.readAllBytes(FILE.toPath()));
			} catch (Exception e) {
				LOGGER.error("Reading dot file failed", e);
				return;
			}
			dotView.getEngine().loadContent("<center>" + content + "</center>");
		});
	}

}
