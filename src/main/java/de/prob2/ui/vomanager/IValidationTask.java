package de.prob2.ui.vomanager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.verifications.type.ValidationTaskTypeResolver;

import javafx.beans.property.ReadOnlyObjectProperty;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "taskType")
@JsonTypeIdResolver(ValidationTaskTypeResolver.class)
@JsonPropertyOrder({ "taskType", "id" })
public interface IValidationTask {

	String getId();

	@JsonIgnore
	ValidationTaskType getTaskType();

	@JsonIgnore
	String getTaskType(I18n i18n);

	@JsonIgnore
	String getTaskDescription(I18n i18n);

	ReadOnlyObjectProperty<Checked> checkedProperty();

	@JsonIgnore
	Checked getChecked();

}
