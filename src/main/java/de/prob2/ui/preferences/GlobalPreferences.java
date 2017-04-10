package de.prob2.ui.preferences;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.MapPropertyBase;
import javafx.collections.FXCollections;

@Singleton
public class GlobalPreferences extends MapPropertyBase<String, String> {
	@Inject
	private GlobalPreferences() {
		super(FXCollections.observableHashMap());
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
