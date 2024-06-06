package de.prob2.ui.preferences;

import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;

import javafx.beans.property.MapPropertyBase;
import javafx.collections.FXCollections;

@Singleton
public final class GlobalPreferences extends MapPropertyBase<String, String> {
	@Inject
	private GlobalPreferences(final Config config) {
		super(FXCollections.observableHashMap());
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.globalPreferences != null) {
					putAll(configData.globalPreferences);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.globalPreferences = new HashMap<>(GlobalPreferences.this);
			}
		});
	}
	
	@Override
	public Object getBean() {
		return null;
	}
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}
}
