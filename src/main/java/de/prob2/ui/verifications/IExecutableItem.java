package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;

public interface IExecutableItem {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
	Checked getChecked();
}
