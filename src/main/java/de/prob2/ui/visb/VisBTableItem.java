package de.prob2.ui.visb;

import de.prob.animator.domainobjects.VisBItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class VisBTableItem {

	private final VisBItem visBItem;

	private BooleanProperty selected;

	public VisBTableItem(final VisBItem visBItem) {
		this.visBItem = visBItem;
		this.selected = new SimpleBooleanProperty(true);
	}

	public VisBItem getVisBItem() {
		return visBItem;
	}

	public BooleanProperty selectedProperty() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}

	public boolean isSelected() {
		return selected.get();
	}
}
