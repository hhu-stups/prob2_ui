package de.prob2.ui;

import java.io.IOException;
import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

@Singleton
public final class AnimationPerspective extends BorderPane {
	// TODO improve DragDrop/Docking
	// TODO remove accordion, if just one element left; add accordion if second object dragged left

	// FIXME drag view model checking

	// TODO? revert to SplitPane
	// TODO? accordion in every direction?
	@FXML
	private OperationsView operations;
	@FXML
	private TitledPane operationsTP;
	@FXML
	private HistoryView history;
	@FXML
	private TitledPane historyTP;
	@FXML
	private ModelcheckingController modelcheck;
	@FXML
	private TitledPane modelcheckTP;
	@FXML
	private AnimationsView animations;
	@FXML
	private TitledPane animationsTP;
	@FXML
	private Accordion accordion;

	private boolean dragged;
	private ImageView snapshot = new ImageView();
	private HashMap<Node, TitledPane> nodeMap = new HashMap<Node, TitledPane>();
	
	@Inject
	private AnimationPerspective(FXMLLoader loader) {
		loader.setLocation(getClass().getResource("animation_perspective.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		double initialHeight = 200;
		double initialWidth = 280;
		operations.setPrefSize(initialWidth,initialHeight);
		history.setPrefSize(initialWidth,initialHeight);
		modelcheck.setPrefSize(initialWidth,initialHeight);
		animations.setPrefSize(initialWidth,initialHeight);
		nodeMap.put(operations,operationsTP);
		nodeMap.put(history,historyTP);
		nodeMap.put(modelcheck,modelcheckTP);
		nodeMap.put(animations,animationsTP);
		onDrag();
	}

	private void onDrag() {
		for (Node node:nodeMap.keySet()){
			registerDrag(node);
		}
	}

	private void registerDrag(final Node node){
		node.setOnMouseEntered(mouseEvent -> this.setCursor(Cursor.OPEN_HAND));
		node.setOnMousePressed(mouseEvent -> this.setCursor(Cursor.CLOSED_HAND));
		node.setOnMouseDragEntered(mouseEvent -> {
			dragged = true;
			mouseEvent.consume();
		});
		node.setOnMouseDragged(mouseEvent -> {
			Point2D position = this.sceneToLocal(new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY()));
			snapshot.relocate(position.getX(), position.getY());
			mouseEvent.consume();
		});
		node.setOnMouseReleased(mouseEvent -> {
			this.setCursor(Cursor.DEFAULT);
			if (dragged){
				dragDropped(node);
			}
			dragged = false;
			snapshot.setImage(null);
			((BorderPane) this.getParent()).getChildren().remove(snapshot);
			mouseEvent.consume();
		});
		node.setOnDragDetected(mouseEvent -> {
			node.startFullDrag();
			SnapshotParameters snapParams = new SnapshotParameters();
			snapParams.setFill(Color.TRANSPARENT);
			snapshot.setImage(node.snapshot(snapParams,null));
			snapshot.setFitWidth(200);
			snapshot.setPreserveRatio(true);
			((BorderPane) this.getParent()).getChildren().add(snapshot);
			mouseEvent.consume();
		});
	}

	private void dragDropped(final Node node){
		//System.out.println(node.getClass().toString() + " dragged, isResizable() = "+node.isResizable());
		TitledPane nodeTP = nodeMap.get(node);
		if (accordion.getPanes().contains(nodeTP)) {
			if (this.getRight() == null) {
				this.setRight(node);
			} else if (this.getTop() == null) {
				this.setTop(node);
			} else if (this.getBottom() == null) {
				this.setBottom(node);
			} else if (this.getLeft() == null) {
				this.setLeft(node);
			}
			nodeTP.setContent(null);
			accordion.getPanes().remove(nodeTP);
			accordion.setExpandedPane(accordion.getPanes().get(0));
			/*if (accordion.getPanes().size()==1){
				this.setLeft(node);
				this.getChildren().remove(accordion);
			}*/
		} else {
			/*if (!this.getChildren().contains(accordion)){
				accordion.getPanes().add(nodeMap.get(this.getLeft()));
				this.setLeft(accordion);
			}*/
			accordion.getPanes().add(nodeTP);
			nodeTP.setContent(node);
			accordion.setExpandedPane(nodeTP);
			this.getChildren().remove(node);
		}
	}
}
