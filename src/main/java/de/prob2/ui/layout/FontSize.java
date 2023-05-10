package de.prob2.ui.layout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.FXMLInjected;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;

@FXMLInjected
@Singleton
public final class FontSize {
	public static final int DEFAULT_FONT_SIZE = 13;
	
	private final IntegerProperty size;
	
	@Inject
	private FontSize(final Config config) {
		this.size = new SimpleIntegerProperty(this, "fontSize", DEFAULT_FONT_SIZE);
		this.size.addListener((o, from, to) -> {
			if (to.intValue() <= 1) {
				this.setFontSize(2);
			}
		});
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.fontSize > 1) {
					setFontSize(configData.fontSize);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.fontSize = getFontSize();
			}
		});
	}
	
	public IntegerProperty fontSizeProperty() {
		return this.size;
	}
	
	public int getFontSize() {
		return this.fontSizeProperty().get();
	}
	
	public void setFontSize(final int fontSize) {
		this.fontSizeProperty().set(fontSize);
	}
	
	public void resetFontSize() {
		this.setFontSize(DEFAULT_FONT_SIZE);
	}
	
	public void applyTo(Node node) {
		node.styleProperty().bind(Bindings.format("-fx-font-size: %dpx;", this.fontSizeProperty()));
	}
}
