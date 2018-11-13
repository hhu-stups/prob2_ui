package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

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

		public Style() {
		}

		public Style(Type shape, Color color, Color lineColor, Double lineWidth, List<Double> lineType) {
			this.shape = shape;
			this.color = color;
			this.lineColor = lineColor;
			this.lineWidth = lineWidth;
			this.lineType = lineType;
		}
	}

	private Text txt;
	private Shape shape;

	public Vertex(String caption, Style style) {
		this(caption, caption, style);
	}

	public Vertex(String id, String caption, Style style) {
		this.setId(id);

		this.txt = new Text(caption);
		Double txtWidth = this.txt.getLayoutBounds().getWidth();
		Double txtHeight = this.txt.getLayoutBounds().getHeight();

		switch (style.shape) {
		case CIRCLE:
			this.shape = new Circle((txtWidth + 20) / 2);
			break;
		case ELLIPSE:
			this.shape = new Ellipse((txtWidth + 30) / 2, (txtHeight + 20) / 2);
			break;
		default:
			this.shape = new Rectangle(txtWidth + 20, txtHeight + 10);
		}

		this.shape.setFill(style.color);
		this.shape.setStroke(style.lineColor);
		this.shape.setStrokeWidth(style.lineWidth);
		this.shape.getStrokeDashArray().addAll(style.lineType);
		
		this.shape.setStrokeLineCap(StrokeLineCap.BUTT);
		this.shape.setStrokeLineJoin(StrokeLineJoin.ROUND);

		this.getChildren().addAll(shape, txt);

		this.setWidth(shape.getLayoutBounds().getWidth());
		this.setHeight(shape.getLayoutBounds().getHeight());
	}

	public double getCenterX() {
		return this.getLayoutX() + shape.getLayoutBounds().getWidth() / 2;
	}

	public double getCenterY() {
		return this.getLayoutY() + shape.getLayoutBounds().getHeight() / 2;
	}

	public double getLeftX() {
		return this.getLayoutX();
	}

	public double getRightX() {
		return this.getLayoutX() + shape.getLayoutBounds().getWidth();
	}

	public double getTopY() {
		return this.getLayoutY();
	}

	public double getBottomY() {
		return this.getLayoutY() + shape.getLayoutBounds().getHeight();
	}
}
