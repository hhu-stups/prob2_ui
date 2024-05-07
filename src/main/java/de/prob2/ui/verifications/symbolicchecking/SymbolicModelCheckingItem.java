package de.prob2.ui.verifications.symbolicchecking;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"selected",
	"algorithm",
})
public final class SymbolicModelCheckingItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicModelCheckingItem.class);
	
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
	public void execute(ExecutionContext context) {
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(getAlgorithm());
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Symbolic model checking interrupted by user", exc);
			this.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}
		
		switch (cmd.getResult()) {
			case SUCCESSFUL:
				this.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.success"));
				break;
			case COUNTER_EXAMPLE:
				this.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.counterExample"));
				break;
			case TIMEOUT:
				this.setResultItem(new CheckingResultItem(Checked.TIMEOUT, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.timeout"));
				break;
			case INTERRUPTED:
				this.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.interrupted"));
				break;
			case LIMIT_REACHED:
				this.setResultItem(new CheckingResultItem(Checked.TIMEOUT, "verifications.symbolicchecking.resultHandler.symbolicChecking.result.limitReached"));
				break;
			default:
				throw new AssertionError("Unhandled symbolic model checking result: " + cmd.getResult());
		}
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
