package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class Vertex extends StackPane {

	public enum Type {
		RECTANGLE, CIRCLE, ELLIPSE
	}

	private Text txt;
	private Shape shape;
	
	public Vertex(String caption) {
		this(caption, caption);
	}
	
	public Vertex(String id, String caption) {
		this(id, Type.RECTANGLE, caption);
	}

	public Vertex(String id, Type type, String caption) {
		this.setId(id);
		
		this.txt = new Text(caption);
		Double txtWidth = this.txt.getLayoutBounds().getWidth();
		Double txtHeight = this.txt.getLayoutBounds().getHeight();

		switch (type) {
		case CIRCLE:
			this.shape = new Circle((txtWidth + 20) / 2);
			break;
		case ELLIPSE:
			this.shape = new Ellipse((txtWidth + 50) / 2, (txtHeight + 50) / 2);
			break;
		default:
			this.shape = new Rectangle(txtWidth + 20, txtHeight + 10);
		}

		this.shape.setFill(Color.WHITE);
		this.shape.setStroke(Color.BLACK);

		this.getChildren().addAll(shape, txt);
		
		this.relocate(Math.random()*750, Math.random()*500);
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
