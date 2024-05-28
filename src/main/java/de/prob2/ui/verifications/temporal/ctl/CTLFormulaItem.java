package de.prob2.ui.verifications.temporal.ctl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class CTLFormulaItem extends TemporalFormulaItem {
	@JsonCreator
	public CTLFormulaItem(
		@JsonProperty("id") String id,
		@JsonProperty("code") String code,
		@JsonProperty("description") String description,
		@JsonProperty("stateLimit") int stateLimit,
		@JsonProperty("expectedResult") boolean expectedResult
	) {
		super(id, code, description, stateLimit, expectedResult);
	}
	
	@Override
	public ValidationTaskType<CTLFormulaItem> getTaskType() {
		return BuiltinValidationTaskTypes.CTL;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.temporal.type.ctl");
	}
	
	@Override
	public void execute(ExecutionContext context) {
		CTLFormulaChecker.checkFormula(this, context.stateSpace());
	}
}
