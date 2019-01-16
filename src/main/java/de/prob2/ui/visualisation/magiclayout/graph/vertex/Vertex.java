package de.prob2.ui.visualisation.magiclayout.graph.vertex;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Text;

public class Vertex extends AbstractVertex {

	public enum Type {
		RECTANGLE, CIRCLE, ELLIPSE, TRIANGLE
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

	private Shape shape;
	private Text txt;

	private Type type;
	private Style style = new Style();

	Vertex() {
	}

	public Vertex(String caption) {
		super();
		this.txt = new Text(caption);
		setType(Type.CIRCLE);
	}

	public String getCaption() {
		return txt.getText();
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
			shape = new Ellipse((txtWidth + 20) / 2, (txtHeight + 20) / 2);
			break;
		case TRIANGLE:
			shape = new Polygon(0, txtHeight + 20, (txtWidth + 30) * 2, txtHeight + 20, txtWidth + 30, 0);
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
		shape.getStrokeDashArray().setAll(style.lineType);
		shape.setStrokeLineCap(StrokeLineCap.BUTT);
		shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
	}

	@Override
	void updateProperties() {
		centerX.set(getLayoutX() + shape.getLayoutBounds().getWidth() / 2.0);
		centerY.set(getLayoutY() + shape.getLayoutBounds().getHeight() / 2.0);
		topY.set(getLayoutY());
		bottomY.set(getLayoutY() + shape.getLayoutBounds().getHeight());
		leftX.set(getLayoutX());
		rightX.set(getLayoutX() + shape.getLayoutBounds().getWidth());

		if (this.type == Type.TRIANGLE) {
			leftX.set(getLayoutX() + shape.getLayoutBounds().getWidth() / 4.0);
			rightX.set(getLayoutX() + shape.getLayoutBounds().getWidth() * 3.0 / 4.0);
		}
	}
}
