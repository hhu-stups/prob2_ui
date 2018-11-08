package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class Node extends StackPane {

	public enum Type {
		RECTANGLE, CIRCLE, ELLIPSE
	}

	Text txt;
	Shape shape;

	public Node(Type type, String caption) {
		this.txt = new Text(caption);
		Double txtWidth = this.txt.getLayoutBounds().getWidth();
		Double txtHeight = this.txt.getLayoutBounds().getHeight();

		switch (type) {
		case CIRCLE:
			this.shape = new Circle(0, 0, (txtWidth + 20) / 2);
			break;
		case ELLIPSE:
			this.shape = new Ellipse((txtWidth + 50) / 2, (txtHeight + 50) / 2);
			break;
		default:
			this.shape = new Rectangle(0, 0, txtWidth + 20, txtHeight + 10);
		}

		this.shape.setFill(Color.WHITE);
		this.shape.setStroke(Color.BLACK);

		this.getChildren().addAll(shape, txt);
	}
}
