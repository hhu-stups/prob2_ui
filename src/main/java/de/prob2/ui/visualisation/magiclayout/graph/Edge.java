package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

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

	public Edge(Vertex source, Vertex target, String caption) {
		this.source = source;
		this.target = target;

		this.txt = new Text(caption);

		Polygon arrowhead = new Polygon(0, 0, 0, 10, Math.sqrt(3) * 5, 5);

		this.getChildren().addAll(line, arrowhead, txt);

		// init Properties
		// when the line moves, also move the text
		centerX.addListener((observable, from, to) -> txt.relocate((double) to, centerY.get()));
		centerY.addListener((observable, from, to) -> txt.relocate(centerX.get(), (double) to));

		// when the line start or end point changes, update the distance and center properties
		ChangeListener<? super Number> lineChangeListener = (observable, from, to) -> {
			distanceX.set(Math.abs(line.getStartX() - line.getEndX()));
			distanceY.set(Math.abs(line.getStartY() - line.getEndY()));
			centerX.set(getDistanceX() / 2 + (line.getStartX() < line.getEndX() ? line.getStartX() : line.getEndX())
					- txt.getLayoutBounds().getWidth() / 2);
			centerY.set(getDistanceY() / 2 + (line.getStartY() < line.getEndY() ? line.getStartY() : line.getEndY())
					- txt.getLayoutBounds().getHeight() / 2);

			// set arrowPoints depending on line end
			arrowhead.getPoints().setAll(line.getEndX(), line.getEndY(), line.getEndX(), line.getEndY() + 10,
					line.getEndX() + Math.sqrt(3) * 5, line.getEndY() + 5);
			
			// rotate arrowhead to point to target node
			Double xDiff = line.getEndX() - line.getStartX();
			Double yDiff = line.getEndY() - line.getStartY();
			Double rotationDegrees = Math.acos(yDiff / Math.sqrt(xDiff * xDiff + yDiff * yDiff)) * 360 / (2 * Math.PI);
			if (xDiff < 0) {
				rotationDegrees = rotationDegrees - 150 ;
			} else {
				rotationDegrees = rotationDegrees * -1 - 150;
			}
			Rotate rotation = new Rotate(rotationDegrees, line.getEndX(), line.getEndY());
			arrowhead.getTransforms().setAll(rotation);
		};
		line.startXProperty().addListener(lineChangeListener);
		line.startYProperty().addListener(lineChangeListener);
		line.endXProperty().addListener(lineChangeListener);
		line.endYProperty().addListener(lineChangeListener);

		distanceX.addListener((observable, from, to) -> calculatePositioning());
		distanceY.addListener((observable, from, to) -> calculatePositioning());

		calculatePositioning();
		setStyle(new Style());
	}
	
	public String getCaption() {
		return txt.getText();
	}
	
	public Vertex getSource() {
		return source;
	}
	
	public Vertex getTarget() {
		return target;
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

	public void setStyle(Style style) {
		this.line.setStroke(style.lineColor);
		this.line.setStrokeWidth(style.lineWidth);
		this.line.getStrokeDashArray().addAll(style.lineType);
		this.line.setStrokeLineCap(StrokeLineCap.BUTT);
		this.line.setStrokeLineJoin(StrokeLineJoin.ROUND);
		this.txt.setFill(style.textColor);
		this.txt.setFont(new Font(style.textSize));
	}
}
