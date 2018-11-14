package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
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

	private Line line = new Line();
	private Text txt;

	private DoubleProperty distanceX = new SimpleDoubleProperty();
	private DoubleProperty distanceY = new SimpleDoubleProperty();
	private DoubleProperty centerX = new SimpleDoubleProperty();
	private DoubleProperty centerY = new SimpleDoubleProperty();

	public Edge(Vertex source, Vertex target, String caption, Style style) {
		this.source = source;
		this.target = target;

		this.line.setStroke(style.lineColor);
		this.line.setStrokeWidth(style.lineWidth);
		this.line.getStrokeDashArray().addAll(style.lineType);
		this.line.setStrokeLineCap(StrokeLineCap.BUTT);
		this.line.setStrokeLineJoin(StrokeLineJoin.ROUND);

		this.txt = new Text(caption);
		this.txt.setFill(style.textColor);
		this.txt.setFont(new Font(style.textSize));
		
		this.getChildren().addAll(line, txt);

		// init Properties
		// when the line moves, also move the text
		centerX.addListener((observable, from, to) -> txt.relocate((double) to, centerY.get())); 
		centerY.addListener((observable, from, to) -> txt.relocate(centerX.get(), (double) to));
		
		// when the line start or end point changes, update the distance and center properties
		ChangeListener<? super Number> distanceChangeListener = (observable, from, to) -> {
			distanceX.set(Math.abs(line.getStartX() - line.getEndX()));
			distanceY.set(Math.abs(line.getStartY() - line.getEndY()));
			centerX.set(getDistanceX() / 2 + (line.getStartX() < line.getEndX() ? line.getStartX() : line.getEndX())
				- txt.getLayoutBounds().getWidth() / 2);
			centerY.set(getDistanceY() / 2 + (line.getStartY() < line.getEndY() ? line.getStartY() : line.getEndY())
				- txt.getLayoutBounds().getHeight() / 2);
		};
		line.startXProperty().addListener(distanceChangeListener);
		line.startYProperty().addListener(distanceChangeListener);
		line.endXProperty().addListener(distanceChangeListener);
		line.endYProperty().addListener(distanceChangeListener);
		
		calculatePositioning();
	}

	public DoubleProperty distanceXProperty() {
		return distanceX;
	}
	
	public double getDistanceX() {
		return distanceX.get();
	}
	
	public DoubleProperty distanceYProperty() {
		return distanceY;
	}

	public double getDistanceY() {
		return distanceY.get();
	}

	private void calculatePositioning() {
		Double approxDistanceX = Math.abs(source.getCenterX() - target.getCenterX());
		Double approxDistanceY = Math.abs(source.getCenterY() - target.getCenterY());

		if (approxDistanceX > approxDistanceY) { // are the vertices rather one below the other or next to each other?
			if (source.getCenterX() > target.getCenterX()) { // which vertex is closer to the origin of the coordinate system?
				line.startXProperty().bind(source.leftXProperty());
				line.endXProperty().bind(target.rightXProperty());
			} else {
				line.startXProperty().bind(source.rightXProperty());
				line.endXProperty().bind(target.leftXProperty());
			}
			line.startYProperty().bind(source.centerYProperty());
			line.endYProperty().bind(target.centerYProperty());
		} else {
			if (source.getCenterY() > target.getCenterY()) { // which vertex is closer to the origin of the coordinate system?
				line.startYProperty().bind(source.topYProperty());
				line.endYProperty().bind(target.bottomYProperty());
			} else {
				line.startYProperty().bind(source.bottomYProperty());
				line.endYProperty().bind(target.topYProperty());
			}
			line.startXProperty().bind(source.centerXProperty());
			line.endXProperty().bind(target.centerXProperty());
		}
	}
}
