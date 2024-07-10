package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;

public interface ISelectableTask extends IValidationTask {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
}
