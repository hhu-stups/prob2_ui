package de.prob2.ui.dynamic.dotty;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class DotFormulaTask extends DynamicFormulaTask<DotFormulaTask> {

	public DotFormulaTask(
		@JsonProperty("id") final String id,
		@JsonProperty("commandType") final String commandType,
		@JsonProperty("formula") final String formula
	) {
		super(id, commandType, formula);
	}

	@Override
	public ValidationTaskType<DotFormulaTask> getTaskType() {
		return BuiltinValidationTaskTypes.DOT_FORMULA;
	}
}
