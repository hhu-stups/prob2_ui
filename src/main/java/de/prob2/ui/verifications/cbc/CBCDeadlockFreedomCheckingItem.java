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
	"predicate",
})
public final class CBCDeadlockFreedomCheckingItem extends SymbolicCheckingFormulaItem {
	private final String predicate;
	
	@JsonCreator
	public CBCDeadlockFreedomCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("predicate") String predicate
	) {
		super(id);
		
		this.predicate = Objects.requireNonNull(predicate, "predicate");
	}
	
	public String getPredicate() {
		return this.predicate;
	}
	
	@Override
	public ValidationTaskType<CBCDeadlockFreedomCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_DEADLOCK_FREEDOM_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.deadlock");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getPredicate();
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCDeadlockFreedomCheckingItem o
			&& this.getPredicate().equals(o.getPredicate());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("predicate", this.getPredicate())
			.toString();
	}
}
