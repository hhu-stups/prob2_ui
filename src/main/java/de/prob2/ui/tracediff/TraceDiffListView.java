package de.prob2.ui.tracediff;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ListView;

public class TraceDiffListView extends ListView<TraceDiffItem> {
	private SimpleBooleanProperty translated = new SimpleBooleanProperty(false);
	SimpleBooleanProperty getTranslatedProperty() {
		return translated;
	}
}
