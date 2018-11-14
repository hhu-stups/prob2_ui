package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Edge extends Group {
	
	public static class Style {
		private Color lineColor = Color.BLACK;
		private Double lineWidth = 1.0;
		private List<Double> lineType = new ArrayList<>();
		private Color textColor = Color.BLACK;
		private Integer textSize = 12; 

		public Style() {
		}

		public Style(Color lineColor, Double lineWidth, List<Double> lineType, Color textColor, Integer textSize) {
			this.lineColor = lineColor;
			this.lineWidth = lineWidth;
			this.lineType = lineType;
			this.textColor = textColor;
			this.textSize = textSize;
		}
	}
	
	private Vertex source;
	private Vertex target;

	private Line line;
	private Text txt;

	public Edge(Vertex source, Vertex target, String caption, Style style) {
		this.source = source;
		this.target = target;
		
		this.line = calculateLine();
		this.line.setStroke(style.lineColor);
		this.line.setStrokeWidth(style.lineWidth);
		this.line.getStrokeDashArray().addAll(style.lineType);
		this.line.setStrokeLineCap(StrokeLineCap.BUTT);
		this.line.setStrokeLineJoin(StrokeLineJoin.ROUND);

		this.txt = new Text(caption);
		this.txt.setFill(style.textColor);
		this.txt.setFont(new Font(style.textSize));
		this.txt.relocate(getCenterX(), getCenterY());

		this.getChildren().addAll(line, txt);
	}

	public double getDistanceX() {
		return Math.abs(line.getStartX() - line.getEndX());
	}

	public double getDistanceY() {
		return Math.abs(line.getStartY() - line.getEndY());
	}

	public double getCenterX() {
		return getDistanceX() / 2 + (line.getStartX() < line.getEndX() ? line.getStartX() : line.getEndX())
				- txt.getLayoutBounds().getWidth() / 2;
	}

	public double getCenterY() {
		return getDistanceY() / 2 + (line.getStartY() < line.getEndY() ? line.getStartY() : line.getEndY())
				- txt.getLayoutBounds().getHeight() / 2;
	}

	private Line calculateLine() {
		Double approxDistanceX = Math.abs(source.getCenterX() - target.getCenterX());
		Double approxDistanceY = Math.abs(source.getCenterY() - target.getCenterY());

		if (approxDistanceX > approxDistanceY) { // are the vertices rather one below the other or next to each other?
			if (source.getCenterX() > target.getCenterX()) { // which vertex is closer to the origin of the coordinate system?
				return new Line(source.getLeftX(), source.getCenterY(), target.getRightX(), target.getCenterY());
			} else {
				return new Line(source.getRightX(), source.getCenterY(), target.getLeftX(), target.getCenterY());
			}
		} else {
			if (source.getCenterY() > target.getCenterY()) { // which vertex is closer to the origin of the coordinate system?
				return new Line(source.getCenterX(), source.getTopY(), target.getCenterX(), target.getBottomY());
			} else {
				return new Line(source.getCenterX(), source.getBottomY(), target.getCenterX(), target.getTopY());
			}
		}
	}
}
