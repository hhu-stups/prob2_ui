package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

public final class WellDefinednessCheckingItem extends SymbolicCheckingFormulaItem {
	@JsonCreator
	public WellDefinednessCheckingItem(@JsonProperty("id") String id) {
		super(id);
	}
	
	@Override
	public ValidationTaskType<WellDefinednessCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.WELL_DEFINEDNESS_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.wellDefinednessChecking");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return "";
	}
}
