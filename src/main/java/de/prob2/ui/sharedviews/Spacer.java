package de.prob2.ui.sharedviews;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A spacer for use in a {@link HBox} or {@link VBox}. A spacer can grow or shrink to any size, and has an {@link Priority#ALWAYS} grow priority by default.
 */
public final class Spacer extends Region {
	public Spacer() {
		super();

		this.setMinWidth(0.0);
		this.setMinHeight(0.0);
		this.setMaxWidth(Double.POSITIVE_INFINITY);
		this.setMaxHeight(Double.POSITIVE_INFINITY);
		HBox.setHgrow(this, Priority.ALWAYS);
		VBox.setVgrow(this, Priority.ALWAYS);
	}
}
