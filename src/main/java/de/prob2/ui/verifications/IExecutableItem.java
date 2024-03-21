package de.prob2.ui.verifications;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

public interface IExecutableItem extends IResettable, IHasSettings {
	boolean selected();
	BooleanProperty selectedProperty();
	void setSelected(boolean selected);
	ReadOnlyObjectProperty<Checked> checkedProperty();
	Checked getChecked();
	void execute(ExecutionContext context);
}
