package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class MagicEdges extends MagicComponent {

	private final ObjectProperty<Color> textColor = new SimpleObjectProperty<>();
	private final IntegerProperty textSize = new SimpleIntegerProperty();

	public MagicEdges(String name, String expression) {
		super(name, expression);

		this.textColor.set(Color.BLACK);
		this.textSize.set(12);
	}

	public MagicEdges(String name) {
		super(name);

		this.textColor.set(Color.BLACK);
		this.textSize.set(12);
	}

	public ObjectProperty<Color> textColorProperty() {
		return textColor;
	}

	public Color getTextColor() {
		return textColor.get();
	}

	public IntegerProperty textSizeProperty() {
		return textSize;
	}

	public int getTextSize() {
		return textSize.get();
	}

	@Override
	public void unbindAll() {
		super.unbindAll();

		textColor.unbind();
		textSize.unbind();
	}
}
