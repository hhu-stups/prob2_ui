package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"selected",
	"operationNames",
})
public final class FindValidStateItem extends SymbolicAnimationItem {
	private final String predicate;
	
	@JsonCreator
	public FindValidStateItem(
		@JsonProperty("predicate") String predicate
	) {
		super();
		
		this.predicate = Objects.requireNonNull(predicate, "predicate");
	}
	
	public String getPredicate() {
		return this.predicate;
	}
	
	@Override
	public ValidationTaskType<?> getTaskType() {
		return BuiltinValidationTaskTypes.FIND_VALID_STATE;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("animation.type.findValidState");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getPredicate();
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof FindValidStateItem o
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
