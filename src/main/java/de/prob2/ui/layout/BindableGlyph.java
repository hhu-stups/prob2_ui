package de.prob2.ui.layout;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.controlsfx.glyphfont.Glyph;

public class BindableGlyph extends Glyph {
	private final DoubleProperty bindableFontSize;
	
	public BindableGlyph() {
		super();
		
		this.bindableFontSize = new SimpleDoubleProperty(this, "bindableFontSize", 1.0);
		this.bindableFontSize.addListener((o, from, to) -> this.setFontSize(to.doubleValue()));
	}

	public BindableGlyph(final String fontFamily, final Object icon) {
		this();

		this.setFontFamily(fontFamily);
		this.setIcon(icon);
	}

	public DoubleProperty bindableFontSizeProperty() {
		return this.bindableFontSize;
	}
	
	public double getBindableFontSize() {
		return this.bindableFontSizeProperty().get();
	}
	
	public void setBindableFontSize(final double bindableFontSize) {
		this.bindableFontSizeProperty().set(bindableFontSize);
	}
}
