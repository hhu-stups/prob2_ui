package de.prob2.ui.verifications.cbc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class CBCRefinementCheckingItem extends SymbolicCheckingFormulaItem {
	@JsonCreator
	public CBCRefinementCheckingItem(@JsonProperty("id") String id) {
		super(id);
	}
	
	@Override
	public ValidationTaskType<CBCRefinementCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_REFINEMENT_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.refinementChecking");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return "";
	}
}
