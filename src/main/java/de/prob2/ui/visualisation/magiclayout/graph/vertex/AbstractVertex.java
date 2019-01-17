package de.prob2.ui.visualisation.magiclayout.graph.vertex;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;

abstract class AbstractVertex extends StackPane {

	DoubleProperty centerX = new SimpleDoubleProperty();
	DoubleProperty centerY = new SimpleDoubleProperty();
	DoubleProperty leftX = new SimpleDoubleProperty();
	DoubleProperty rightX = new SimpleDoubleProperty();
	DoubleProperty topY = new SimpleDoubleProperty();
	DoubleProperty bottomY = new SimpleDoubleProperty();

	AbstractVertex() {
		this.layoutXProperty().addListener((observable, from, to) -> updateProperties());
		this.layoutYProperty().addListener((observable, from, to) -> updateProperties());

		this.setCursor(Cursor.HAND);
		this.setOnMouseDragged(event -> {
			this.setLayoutX(this.getLayoutX() + event.getX() - this.getWidth() / 2);
			this.setLayoutY(this.getLayoutY() + event.getY() - this.getHeight() / 2);
		});
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

	abstract void updateProperties();

}
