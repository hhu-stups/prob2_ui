package de.prob2.ui.dynamic.plantuml;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.dynamic.DynamicFormulaTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class PlantUmlFormulaTask extends DynamicFormulaTask<PlantUmlFormulaTask> {

	public PlantUmlFormulaTask(
		@JsonProperty("id") final String id,
		@JsonProperty("commandType") final String commandType,
		@JsonProperty("formula") final String formula
	) {
		super(id, commandType, formula);
	}

	@Override
	public ValidationTaskType<PlantUmlFormulaTask> getTaskType() {
		return BuiltinValidationTaskTypes.PLANTUML_FORMULA;
	}
}
