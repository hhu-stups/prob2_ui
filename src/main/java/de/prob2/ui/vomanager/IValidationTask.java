package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;

import javafx.beans.property.ReadOnlyObjectProperty;

public interface IValidationTask {
	String getId();
	@JsonIgnore
	String getTaskType(I18n i18n);
	@JsonIgnore
	String getTaskDescription(I18n i18n);
	ReadOnlyObjectProperty<Checked> checkedProperty();
	@JsonIgnore
	Checked getChecked();
}
