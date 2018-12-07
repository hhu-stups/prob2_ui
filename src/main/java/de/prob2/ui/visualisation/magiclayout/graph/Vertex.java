package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Text;

public class Vertex extends StackPane {

	public enum Type {
		RECTANGLE, CIRCLE, ELLIPSE, TRIANGLE, DUMMY
	}

	public static class Style {
		private Color color = Color.WHITE;
		private Color lineColor = Color.BLACK;
		private Double lineWidth = 1.0;
		private List<Double> lineType = new ArrayList<>();
		private Color textColor = Color.BLACK;

		public Style() {
		}

		public Style(Color color, Color lineColor, Double lineWidth, List<Double> lineType, Color textColor) {
			this.color = color;
			this.lineColor = lineColor;
			this.lineWidth = lineWidth;
			this.lineType = lineType;
			this.textColor = textColor;
		}
	}

	private Text txt;
	private Shape shape;

	private Type type;
	private Style style = new Style();

	private DoubleProperty centerX = new SimpleDoubleProperty();
	private DoubleProperty centerY = new SimpleDoubleProperty();
	private DoubleProperty leftX = new SimpleDoubleProperty();
	private DoubleProperty rightX = new SimpleDoubleProperty();
	private DoubleProperty topY = new SimpleDoubleProperty();
	private DoubleProperty bottomY = new SimpleDoubleProperty();

	public Vertex(String caption) {
		this.txt = new Text(caption);
		setType(Type.CIRCLE);

		this.layoutXProperty().addListener((observable, from, to) -> updateProperties());
		this.layoutYProperty().addListener((observable, from, to) -> updateProperties());

		this.setCursor(Cursor.HAND);
		this.setOnMouseDragged(event -> {
			this.setLayoutX(this.getLayoutX() + event.getX() - this.getWidth() / 2);
			this.setLayoutY(this.getLayoutY() + event.getY() - this.getHeight() / 2);
		});
	}

	public String getCaption() {
		return txt.getText();
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

	public void setType(Type type) {
		this.type = type;

		Double txtWidth = txt.getLayoutBounds().getWidth();
		Double txtHeight = txt.getLayoutBounds().getHeight();

		switch (type) {
		case RECTANGLE:
			shape = new Rectangle(txtWidth + 20, txtHeight + 10);
			break;
		case CIRCLE:
			shape = new Circle((txtWidth + 20) / 2);
			break;
		case ELLIPSE:
			shape = new Ellipse((txtWidth + 30) / 2, (txtHeight + 20) / 2);
			break;
		case TRIANGLE:
			shape = new Polygon(0, txtHeight + 20, (txtWidth + 30) * 2, txtHeight + 20, txtWidth + 30, 0);
			break;
		case DUMMY:
			shape = new Circle(0);
			break;
		default:
			shape = new Circle((txtWidth + 20) / 2);
		}

		setStyle(this.style);

		this.setWidth(shape.getLayoutBounds().getWidth());
		this.setHeight(shape.getLayoutBounds().getHeight());

		// relocate vertex so that its center stays the same
		this.relocate(getCenterX() - getWidth() / 2, getCenterY() - getHeight() / 2);

		this.getChildren().setAll(shape, txt);
		updateProperties();
	}

	public Type getType() {
		return type;
	}

	public void setStyle(Style style) {
		this.style = style;

		txt.setFill(style.textColor);

		shape.setFill(style.color);
		shape.setStroke(style.lineColor);
		shape.setStrokeWidth(style.lineWidth);
		shape.getStrokeDashArray().addAll(style.lineType);
		shape.setStrokeLineCap(StrokeLineCap.BUTT);
		shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
		
		if(type == Type.DUMMY) {
			shape.setStrokeWidth(0);
		}
	}

	private void updateProperties() {
		centerX.set(getLayoutX() + shape.getLayoutBounds().getWidth() / 2);
		centerY.set(getLayoutY() + shape.getLayoutBounds().getHeight() / 2);
		topY.set(getLayoutY());
		bottomY.set(getLayoutY() + shape.getLayoutBounds().getHeight());

		if (this.type == Type.TRIANGLE) {
			leftX.set(getLayoutX() + shape.getLayoutBounds().getWidth() / 4);
			rightX.set(getLayoutX() + shape.getLayoutBounds().getWidth() * 3 / 4);
		} else {
			leftX.set(getLayoutX());
			rightX.set(getLayoutX() + shape.getLayoutBounds().getWidth());
		}
	}
}
