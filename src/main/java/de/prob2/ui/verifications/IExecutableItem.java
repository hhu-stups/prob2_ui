package de.prob2.ui.verifications;

import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.BooleanProperty;

public interface IExecutableItem extends IValidationTask {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
}
