package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class MagicEdges extends MagicComponent {

	private final IntegerProperty textSize = new SimpleIntegerProperty();

	public MagicEdges(String name, String expression) {
		super(name, expression);

		this.textSize.set(12);
	}

	public MagicEdges(String name) {
		super(name, "");
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

		textSize.unbind();
	}
}
