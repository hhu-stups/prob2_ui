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

		Double distanceX = Math.abs(source.getCenterX() - target.getCenterX());
		Double distanceY = Math.abs(source.getCenterY() - target.getCenterY());

		if (distanceX > distanceY) {
			if (source.getCenterX() > target.getCenterX()) {
				this.line = new Line(source.getLeftX(), source.getCenterY(), target.getRightX(), target.getCenterY());
			} else {
				this.line = new Line(source.getRightX(), source.getCenterY(), target.getLeftX(), target.getCenterY());
			}
		} else {
			if (source.getCenterY() > target.getCenterY()) {
				this.line = new Line(source.getCenterX(), source.getTopY(), target.getCenterX(), target.getBottomY());
			} else {
				this.line = new Line(source.getCenterX(), source.getBottomY(), target.getCenterX(), target.getTopY());
			}
		}
		this.txt = new Text(caption);
		this.txt.relocate(
				distanceX / 2 + (source.getCenterX() > target.getCenterX() ? target.getCenterX() : source.getCenterX()),
				distanceY / 2
						+ (source.getCenterY() > target.getCenterY() ? target.getCenterY() : source.getCenterY()));

		this.getChildren().addAll(line, txt);
	}

}
