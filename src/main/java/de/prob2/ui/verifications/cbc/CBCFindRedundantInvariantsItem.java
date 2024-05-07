package de.prob2.ui.verifications.cbc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class CBCFindRedundantInvariantsItem extends SymbolicCheckingFormulaItem {
	@JsonCreator
	public CBCFindRedundantInvariantsItem(@JsonProperty("id") String id) {
		super(id);
	}
	
	@Override
	public ValidationTaskType<CBCFindRedundantInvariantsItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_FIND_REDUNDANT_INVARIANTS;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.findRedundantInvariants");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return "";
	}
}
