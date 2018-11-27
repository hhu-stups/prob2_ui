package de.prob2.ui.visualisation.magiclayout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.editPane.MagicLayoutEditEdges;
import de.prob2.ui.visualisation.magiclayout.editPane.MagicLayoutEditNodes;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@Singleton
public class MagicLayoutView extends Stage {

	@FXML
	private MenuBar menuBar;
	@FXML
	private TabPane editTabPane;
	@FXML
	private Tab editNodesTab;
	@FXML
	private Tab editEdgesTab;
	@FXML
	private MagicLayoutEditNodes magicLayoutEditNodes;
	@FXML
	private MagicLayoutEditEdges magicLayoutEditEdges;
	@FXML
	private StackPane magicGraphStackPane; // The StackPane which contains a group which contains the magicGraphPane
	@FXML
	private StackPane magicGraphPane;
	@FXML
	private Button zoomInButton;
	@FXML
	private Button zoomOutButton;

	private final StageManager stageManager;

	@Inject
	public MagicLayoutView(final StageManager stageManager) {
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "magic_layout_view.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.setMacMenuBar(this, menuBar);

		// make GraphPane zoomable
		magicGraphPane.setOnZoom(event -> zoom(event.getZoomFactor()));
		magicGraphStackPane.setOnZoom(event -> zoom(event.getZoomFactor())); // recognize zoom Motion outside the magicGraphPane area
		zoomInButton.setOnAction(event -> zoom(1.1));
		zoomOutButton.setOnAction(event -> zoom(0.9));
	}
	
	private void zoom(double zoomFactor) {
		magicGraphPane.setScaleX(magicGraphPane.getScaleX() * zoomFactor);
		magicGraphPane.setScaleY(magicGraphPane.getScaleY() * zoomFactor);
	}

	@FXML
	private void newNodeGroup() {
		editTabPane.getSelectionModel().select(editNodesTab);
		magicLayoutEditNodes.addNodes();
	}

	@FXML
	private void newEdgeGroup() {
		editTabPane.getSelectionModel().select(editEdgesTab);
		magicLayoutEditEdges.addEdges();
	}
}
