package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.IExecutableItem;

public interface IValidationTask extends IExecutableItem {
	String getId();
	@JsonIgnore
	String getTaskDescription(I18n i18n);
}
