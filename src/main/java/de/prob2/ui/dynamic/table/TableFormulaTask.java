package de.prob2.ui.dynamic.table;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class TableFormulaTask extends DynamicFormulaTask {
	public TableFormulaTask(
		@JsonProperty("id") final String id,
		@JsonProperty("commandType") final String commandType,
		@JsonProperty("formula") final String formula
	) {
		super(id, commandType, formula);
	}

	@Override
	public ValidationTaskType<TableFormulaTask> getTaskType() {
		return BuiltinValidationTaskTypes.TABLE_FORMULA;
	}
}
