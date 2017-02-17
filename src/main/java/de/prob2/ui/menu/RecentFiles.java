package de.prob2.ui.menu;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

@Singleton
public final class RecentFiles {
	private final IntegerProperty maximum = new SimpleIntegerProperty(this, "maximum");
	private SimpleListProperty<String> recentFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
	private SimpleListProperty<String> recentProjects = new SimpleListProperty<>(FXCollections.observableArrayList());
	
	@Inject
	private RecentFiles() {
		ListChangeListener<? super String> listener = change -> {
			if (change.getList().size() > this.getMaximum()) {
				// Truncate the list of recent files if it is longer than the maximum
				change.getList().remove(this.getMaximum(), change.getList().size());
			}
		};
		this.recentFiles.addListener(listener);
		this.recentProjects.addListener(listener);
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
	
	public SimpleListProperty<String> recentFilesProperty() {
		return this.recentFiles;
	}
	
	public List<String> getRecentFiles() {
		return this.recentFilesProperty().get();
	}

	public void setRecentFiles(List<String> recentFiles) {
		this.recentFilesProperty().setAll(recentFiles);
	}

	public SimpleListProperty<String> recentProjectsProperty() {
		return this.recentProjects;
	}
	
	public List<String> getRecentProjects() {
		return this.recentProjectsProperty().get();
	}

	public void setRecentProjects(List<String> recentProjects) {
		this.recentProjectsProperty().setAll(recentProjects);
	}
}
