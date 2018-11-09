package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class Edge extends Group {
	private Vertex source;
	private Vertex target;

	private Line line;
	private Text txt;

	public Edge(Vertex source, Vertex target, String caption) {
		this.source = source;
		this.target = target;

		this.line = new Line(source.getLayoutX(), source.getLayoutY(), target.getLayoutX(),
				target.getLayoutY());
		
		this.txt = new Text(caption);
		
		this.getChildren().addAll(line, txt);
	}
	
	
}
