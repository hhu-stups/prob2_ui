package de.prob2.ui.verifications.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import de.prob2.ui.vomanager.IValidationTask;

public final class ValidationTaskTypeResolver extends TypeIdResolverBase {

	@Override
	public String idFromValue(Object value) {
		return ((IValidationTask) value).getTaskType().getKey();
	}

	@Override
	public String idFromValueAndType(Object value, Class<?> suggestedType) {
		return idFromValue(value);
	}

	@Override
	public JsonTypeInfo.Id getMechanism() {
		return JsonTypeInfo.Id.CUSTOM;
	}

	@Override
	public JavaType typeFromId(DatabindContext context, String id) {
		return context.constructType(ValidationTaskType.get(id).getTaskClass());
	}
}
