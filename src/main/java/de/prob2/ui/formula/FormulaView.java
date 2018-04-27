package de.prob2.ui.formula;


import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.transform.Scale;

public class FormulaView extends Group {
	
	private FormulaGraph graph;
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.84;
	
	public FormulaView(FormulaGraph data) {
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
	}
	
	public void zoomByFactor(double factor) {
		graph.getTransforms().add(new Scale(factor, factor));
		dragFactor *= factor;
	}
	
	public void defaultSize() {
		graph.getTransforms().clear();
	}
		
}
