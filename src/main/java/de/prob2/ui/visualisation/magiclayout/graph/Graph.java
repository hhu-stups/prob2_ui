package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class Graph extends Pane {

	public Graph() {
		Vertex cir = new Vertex(Vertex.Type.CIRCLE, "I'm a Circle.");
		cir.relocate(50, 50);
		
		Vertex el = new Vertex(Vertex.Type.ELLIPSE, "I'm a Ellipse.");
		el.relocate(150, 150);
		
		this.getChildren().addAll(new Vertex(Vertex.Type.RECTANGLE, "I'm a Rectangle."), cir, el);
	}

	@FXML
	public void initialize() {
	}
}
