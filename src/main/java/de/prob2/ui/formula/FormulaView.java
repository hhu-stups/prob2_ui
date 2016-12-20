package de.prob2.ui.formula;

import de.prob2.ui.internal.StageManager;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class FormulaView extends Stage {
	
	private Group group;
	private ScrollPane root;
	private FormulaGraph graph;
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.84;
	
	public FormulaView(StageManager stageManager, FormulaGraph data) {
		super();
		graph = data;
		setEventListeners();
		group = new Group();
		root = new ScrollPane(group);
		group.getChildren().add(graph);
		// Wrap root in a StackPane so the Mac menu bar can be set
		// (the root has to be a Pane subclass, and ScrollPane extends Control and not Pane)
		this.setScene(new Scene(new StackPane(root), 1024, 768));
		this.setTitle("Mathematical Expression");
		stageManager.register(this, null);
	}
	
	private void setEventListeners() {
		graph.setOnMouseMoved(e-> {
			graph.setCursor(Cursor.HAND);
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		graph.setOnMouseDragged(e-> {
			graph.setCursor(Cursor.MOVE);
			root.setHvalue(root.getHvalue() + (-e.getSceneX() + oldMousePositionX)/(graph.getWidth() * dragFactor));
			root.setVvalue(root.getVvalue() + (-e.getSceneY() + oldMousePositionY)/(graph.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();

		});
		graph.setOnMouseClicked(e -> {
			if(e.getClickCount() < 2) {
				return;
			}

			if(e.getButton() == MouseButton.PRIMARY) {
				graph.getTransforms().add(new Scale(1.3, 1.3));	
				dragFactor *= 1.3;
			} else if(e.getButton() == MouseButton.SECONDARY) {
				graph.getTransforms().add(new Scale(0.8, 0.8));
				dragFactor *= 0.8;
			}
			
			group.getChildren().clear();
			group.getChildren().add(graph);
			root.setHvalue(e.getX() / graph.getWidth());
			root.setVvalue(e.getY() / graph.getHeight());
		});
	}
}
