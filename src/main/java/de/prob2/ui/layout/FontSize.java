package de.prob2.ui.layout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.SimpleIntegerProperty;

@Singleton
public class FontSize extends SimpleIntegerProperty {

	private int defaultFontSize = 13;
	
	@Inject
	public FontSize() {
		this.setDefault();
	}
	
	@Override
	public void set(int newValue) {
		if(newValue <= 1) {
			super.set(2);
		} else {
			super.set(newValue);
		}
		
	}
	
	public void setDefault() {
		this.set(defaultFontSize);
	}
}
