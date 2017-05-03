package de.prob2.ui.layout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.SimpleIntegerProperty;

@Singleton
public class FontSize extends SimpleIntegerProperty {

	@Inject
	public FontSize() {
		this.set(10);
	}
}
