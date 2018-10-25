package de.prob2.ui.visualisation.magiclayout;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class MagicEdge extends MagicComponent {
	
	private ObjectProperty<Color> textColor = new SimpleObjectProperty<>();
	private IntegerProperty textSize = new SimpleIntegerProperty();
	
	public MagicEdge(String name, String expression) {
		super(name, expression);
		
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
