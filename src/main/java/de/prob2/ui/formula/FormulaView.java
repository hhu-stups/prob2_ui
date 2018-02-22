package de.prob2.ui.formula;

import java.util.ResourceBundle;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.transform.Scale;

public class FormulaView extends Group {
	
	private FormulaGraph graph;
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.84;
	
	public FormulaView(FormulaGraph data, ResourceBundle bundle) {
		graph = data;
		setEventListeners();
		this.getChildren().add(graph);
	}
	
	private void setEventListeners() {
		graph.setOnMouseMoved(e-> {
			graph.setCursor(Cursor.HAND);
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		graph.setOnMouseDragged(e-> {
			ScrollPane parent = (ScrollPane) this.getParent().getParent().getParent();
			graph.setCursor(Cursor.MOVE);
			parent.setHvalue(parent.getHvalue() + (-e.getSceneX() + oldMousePositionX)/(graph.getWidth() * dragFactor));
			parent.setVvalue(parent.getVvalue() + (-e.getSceneY() + oldMousePositionY)/(graph.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		graph.setOnMouseClicked(e -> {
			ScrollPane parent = (ScrollPane) this.getParent().getParent().getParent();
			if (e.getClickCount() < 2) {
				return;
			}

			if (e.getButton() == MouseButton.PRIMARY) {
				graph.getTransforms().add(new Scale(1.3, 1.3));
				dragFactor *= 1.3;
			} else if (e.getButton() == MouseButton.SECONDARY) {
				graph.getTransforms().add(new Scale(0.8, 0.8));
				dragFactor *= 0.8;
			}
			this.getChildren().clear();
			this.getChildren().add(graph);
			parent.setHvalue(e.getX() / graph.getWidth());
			parent.setVvalue(e.getY() / graph.getHeight());
		});
	}
}
