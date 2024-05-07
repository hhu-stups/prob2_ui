package de.prob2.ui.verifications.symbolicchecking;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"id",
	"selected",
	"algorithm",
})
public final class SymbolicModelCheckingItem extends SymbolicCheckingFormulaItem {
	private final SymbolicModelcheckCommand.Algorithm algorithm;
	
	@JsonCreator
	public SymbolicModelCheckingItem(
		@JsonProperty("id") String id,
		@JsonProperty("algorithm") SymbolicModelcheckCommand.Algorithm algorithm
	) {
		super(id);
		
		this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
	}
	
	public SymbolicModelcheckCommand.Algorithm getAlgorithm() {
		return this.algorithm;
	}
	
	@Override
	public ValidationTaskType<SymbolicModelCheckingItem> getTaskType() {
		return BuiltinValidationTaskTypes.SYMBOLIC_MODEL_CHECKING;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("verifications.symbolicchecking.type.symbolicModelChecking");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return this.getAlgorithm().name();
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof SymbolicModelCheckingItem o
			&& this.getAlgorithm().equals(o.getAlgorithm());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("algorithm", this.getAlgorithm())
			.toString();
	}
}
