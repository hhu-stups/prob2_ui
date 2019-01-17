package de.prob2.ui.visualisation.magiclayout.graph.vertex;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class DummyVertex extends Vertex {

	Circle circle = new Circle(5);

	public DummyVertex() {
		this.getChildren().setAll(circle);
		circle.setFill(Color.TRANSPARENT);
	}

	@Override
	void updateProperties() {
		centerX.set(getLayoutX() + circle.getLayoutBounds().getWidth() / 2.0);
		centerY.set(getLayoutY() + circle.getLayoutBounds().getHeight() / 2.0);
		topY.set(getCenterY());
		bottomY.set(getCenterY());
		leftX.set(getCenterX());
		rightX.set(getCenterX());
	}

	@Override
	public String getCaption() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setStyle(Style style) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setType(Type type) {
		throw new UnsupportedOperationException();
	}
}
