package de.prob2.ui.menu;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

@Singleton
public final class RecentProjects extends SimpleListProperty<String> {
	private final IntegerProperty maximum = new SimpleIntegerProperty(this, "maximum");
	
	@Inject
	private RecentProjects(final Config config) {
		this.set(FXCollections.observableArrayList());
		this.addListener((ListChangeListener<? super String>)change -> {
			if (change.getList().size() > this.getMaximum()) {
				// Truncate the list of recent files if it is longer than the maximum
				change.getList().remove(this.getMaximum(), change.getList().size());
			}
		});
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.maxRecentProjects > 0) {
					setMaximum(configData.maxRecentProjects);
				} else {
					setMaximum(10);
				}
				
				if (configData.recentProjects != null) {
					setAll(configData.recentProjects);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.maxRecentProjects = getMaximum();
				configData.recentProjects = new ArrayList<>(RecentProjects.this);
			}
		});
	}
	
	public IntegerProperty maximumProperty() {
		return this.maximum;
	}
	
	public int getMaximum() {
		return this.maximumProperty().get();
	}
	
	public void setMaximum(final int maximum) {
		this.maximumProperty().set(maximum);
	}
}
