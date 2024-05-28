package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.verifications.type.ValidationTaskTypeResolver;

import javafx.beans.property.ReadOnlyObjectProperty;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "taskType")
@JsonTypeIdResolver(ValidationTaskTypeResolver.class)
public interface IValidationTask {
	String getId();

	@JsonIgnore
	ValidationTaskType<?> getTaskType();

	@JsonIgnore
	String getTaskType(I18n i18n);

	@JsonIgnore
	String getTaskDescription(I18n i18n);

	ReadOnlyObjectProperty<Checked> checkedProperty();

	@JsonIgnore
	Checked getChecked();

	void reset();

	/**
	 * This method should be used to check equality for serialisation and editing.
	 * All of these properties should be final!
	 * Properties that are mutable, transient, derived or only used for caching should be ignored.
	 *
	 * <ul>
	 *     <li>two tasks which have different IDs but are otherwise the same should return false</li>
	 *     <li>two tasks which have different "checked" status but are otherwise the same should return true</li>
	 * </ul>
	 *
	 * @param other other object
	 * @return true iff this == that wrt the constraints above
	 */
	boolean settingsEqual(Object other);
}
