package de.prob2.ui.verifications.cbc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"id",
	"selected",
	"operationName",
})
public final class CBCInvariantPreservationCheckingItem extends SymbolicCheckingFormulaItem {
	private final String operationName;
	
	@JsonCreator
	public CBCInvariantPreservationCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("operationName") String operationName
	) {
		super(id);
		
		this.operationName = operationName;
	}
	
	public String getOperationName() {
		return this.operationName;
	}
	
	@Override
	public ValidationTaskType<CBCInvariantPreservationCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_INVARIANT_PRESERVATION_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.invariant");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		if (this.getOperationName() == null) {
			return i18n.translate("verifications.symbolicchecking.choice.checkAllOperations");
		} else {
			return this.getOperationName();
		}
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCInvariantPreservationCheckingItem o
			&& Objects.equals(this.getOperationName(), o.getOperationName());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("operationName", this.getOperationName())
			.toString();
	}
}
