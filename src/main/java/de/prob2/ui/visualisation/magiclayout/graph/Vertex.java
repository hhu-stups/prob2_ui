package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Text;

public class Vertex extends StackPane {

	public enum Type {
		RECTANGLE, CIRCLE, ELLIPSE
	}

	public static class Style {
		private Type shape = Type.RECTANGLE;
		private Color color = Color.WHITE;
		private Color lineColor = Color.BLACK;
		private Double lineWidth = 1.0;
		private List<Double> lineType = new ArrayList<>();
		private Color textColor = Color.BLACK;

		public Style() {
		}

		public Style(Type shape, Color color, Color lineColor, Double lineWidth, List<Double> lineType, Color textColor) {
			this.shape = shape;
			this.color = color;
			this.lineColor = lineColor;
			this.lineWidth = lineWidth;
			this.lineType = lineType;
			this.textColor = textColor;
		}
	}

	private Text txt;
	private Shape shape;
	
	private DoubleProperty centerX = new SimpleDoubleProperty();
	private DoubleProperty centerY = new SimpleDoubleProperty();
	private DoubleProperty leftX = new SimpleDoubleProperty();
	private DoubleProperty rightX = new SimpleDoubleProperty();
	private DoubleProperty topY = new SimpleDoubleProperty();
	private DoubleProperty bottomY = new SimpleDoubleProperty();

	public Vertex(String caption, Style style) {
		this(caption, caption, style);
	}

	public Vertex(String id, String caption, Style style) {
		this.setId(id);

		this.txt = new Text(caption);
		
		updateStyle(style);
	}

	public DoubleProperty centerXProperty() {
		return centerX;
	}
	
 	public double getCenterX() {
		return centerX.get();
	}

 	public DoubleProperty centerYProperty() {
		return centerY;
	}
 	
	public double getCenterY() {
		return centerY.get();
	}
	
	public DoubleProperty leftXProperty() {
		return leftX;
	}

	public double getLeftX() {
		return leftX.get();
	}

	public DoubleProperty rightXProperty() {
		return rightX;
	}
	
	public double getRightX() {
		return rightX.get();
	}
	
	public DoubleProperty topYProperty() {
		return topY;
	}

	public double getTopY() {
		return topY.get();
	}
	
	public DoubleProperty bottomYProperty() {
		return bottomY;
	}

	public double getBottomY() {
		return bottomY.get();
	}

	public void updateStyle(Style style) {
		txt.setFill(style.textColor);
		
		Double txtWidth = txt.getLayoutBounds().getWidth();
		Double txtHeight = txt.getLayoutBounds().getHeight();

		switch (style.shape) {
		case CIRCLE:
			shape = new Circle((txtWidth + 20) / 2);
			break;
		case ELLIPSE:
			shape = new Ellipse((txtWidth + 30) / 2, (txtHeight + 20) / 2);
			break;
		default:
			shape = new Rectangle(txtWidth + 20, txtHeight + 10);
		}

		shape.setFill(style.color);
		shape.setStroke(style.lineColor);
		shape.setStrokeWidth(style.lineWidth);
		shape.getStrokeDashArray().addAll(style.lineType);
		shape.setStrokeLineCap(StrokeLineCap.BUTT);
		shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
		
		this.setWidth(shape.getLayoutBounds().getWidth());
		this.setHeight(shape.getLayoutBounds().getHeight());
		
		this.getChildren().setAll(shape, txt);
		
		updateProperties();
	}

	private void updateProperties() {
		centerX.set(getLayoutX() + shape.getLayoutBounds().getWidth() / 2);
		centerY.set(getLayoutY() + shape.getLayoutBounds().getHeight() / 2);
		leftX.set(getLayoutX());
		rightX.set(getLayoutX() + shape.getLayoutBounds().getWidth());
		topY.set(getLayoutY());
		bottomY.set(getLayoutY() + shape.getLayoutBounds().getHeight());
	}
}
