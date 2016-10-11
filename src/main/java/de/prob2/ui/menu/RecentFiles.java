package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

@Singleton
public final class RecentFiles extends SimpleListProperty<String> {
	private final IntegerProperty maximum;
	
	@Inject
	private RecentFiles() {
		this.set(FXCollections.observableArrayList());
		this.maximum = new SimpleIntegerProperty(this, "maximum");
		this.addListener((ListChangeListener<? super String>)change -> {
			if (change.getList().size() > this.getMaximum()) {
				// Truncate the list of recent files if it is longer than the maximum
				change.getList().remove(this.getMaximum(), change.getList().size());
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
