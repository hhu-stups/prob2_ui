package de.prob2.ui.dotty;


import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.exception.ProBError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DotView extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);
	
	@FXML
	private WebView dotView;
	
	@FXML
	private ChoiceBox<DotChoiceItem> cbChoice;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private ScrollPane pane;
	
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 1;
	
	private final CurrentTrace currentTrace;
	
	private static final File FILE = new File(Main.getProBDirectory()
			+ File.separator + "prob2ui" + File.separator + "out.svg");
	
	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace) {
		stageManager.loadFXML(this, "dot_view.fxml");
		this.currentTrace = currentTrace;
	}
	
	@FXML
	public void initialize() {
		dotView.setContextMenuEnabled(false);
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
				dotView.setZoom(dotView.getZoom() * 0.5);
				dragFactor *= 0.8;
			} else {
				dotView.setZoom(dotView.getZoom() * 2);
				dragFactor *= 1.3;
			}
			pane.setHvalue(e.getX() / pane.getWidth());
			pane.setVvalue(e.getY() / pane.getHeight());
		});
		cbChoice.getSelectionModel().selectFirst();
		cbChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> 
			tfFormula.setVisible(to.hasFormula())
		);
	}
	
	@FXML
	public void visualize() {
		ArrayList<String> formulas = new ArrayList<>();
		if(cbChoice.getSelectionModel().getSelectedItem().hasFormula()) {
			formulas.add(tfFormula.getText());
		}
		GetSvgForVisualizationCommand cmd = new GetSvgForVisualizationCommand(cbChoice.getValue().geVisualisationType().getOption(), FILE, formulas);
		try {
			currentTrace.getStateSpace().execute(cmd);
			dotView.getEngine().load(FILE.toURI().toString());
		} catch(ProBError e) {
			LOGGER.error(e.getMessage());
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Visualisation not possible!");
			alert.setTitle("Graph Visualisation not possible!");
			alert.setContentText(e.getMessage());
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			alert.showAndWait();
		}
	}

}
