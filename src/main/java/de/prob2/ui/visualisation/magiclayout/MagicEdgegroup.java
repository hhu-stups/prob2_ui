package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class MagicEdgegroup extends MagicComponent {

	private final IntegerProperty textSize = new SimpleIntegerProperty();

	public MagicEdgegroup(String name, String expression) {
		super(name, expression);

		this.textSize.set(12);
	}

	public MagicEdgegroup(String name) {
		this(name, "");
	}
	
	public MagicEdgegroup(MagicEdgegroup edges) {
		super(edges);
		
		this.textSize.set(edges.getTextSize());
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
	
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
