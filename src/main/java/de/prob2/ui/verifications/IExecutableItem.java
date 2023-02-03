package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

public interface IExecutableItem {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
	ReadOnlyObjectProperty<Checked> checkedProperty();
	Checked getChecked();
	boolean settingsEqual(IExecutableItem obj);
}
