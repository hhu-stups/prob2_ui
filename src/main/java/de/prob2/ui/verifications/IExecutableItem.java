package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;

public interface IExecutableItem {
	boolean shouldExecute();
	BooleanProperty shouldExecuteProperty();
	void setShouldExecute(boolean shouldExecute);
	Checked getChecked();
}
