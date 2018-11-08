package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class Graph extends Pane {

	public Graph() {
		Node cir = new Node(Node.Type.CIRCLE, "I'm a Circle.");
		cir.relocate(50, 50);
		
		Node el = new Node(Node.Type.ELLIPSE, "I'm a Ellipse.");
		el.relocate(150, 150);
		
		this.getChildren().addAll(new Node(Node.Type.RECTANGLE, "I'm a Rectangle."), cir, el);
	}

	@FXML
	public void initialize() {
	}
}
