package de.prob2.ui.layout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.SimpleIntegerProperty;

@Singleton
public class FontSize extends SimpleIntegerProperty {

	@Inject
	public FontSize() {
		this.set(13);
	}
	
	@Override
	public void set(int newValue) {
		if(newValue <= 0) {
			newValue = 1;
		}
		super.set(newValue);
		System.out.println(newValue);
	}
}
