package de.prob2.ui.animation.symbolic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"selected",
	"operationNames",
})
public final class FindValidStateItem extends SymbolicAnimationItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(FindValidStateItem.class);
	
	private final String predicate;
	
	@JsonCreator
	public FindValidStateItem(
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
	public void execute(ExecutionContext context) {
		this.setExample(null);
		
		FindStateCommand cmd = new FindStateCommand(context.stateSpace(), new ClassicalB(getPredicate()), true);
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Find valid state interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}
		
		switch (cmd.getResult()) {
			case STATE_FOUND:
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "animation.symbolic.resultHandler.findValidState.result.found"));
				this.setExample(cmd.getTrace(context.stateSpace()));
				break;
			case NO_STATE_FOUND:
				this.setResult(new CheckingResult(CheckingStatus.FAIL, "animation.symbolic.resultHandler.findValidState.result.notFound"));
				break;
			case INTERRUPTED:
				this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "animation.symbolic.resultHandler.findValidState.result.interrupted"));
				break;
			case ERROR:
				this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "animation.symbolic.resultHandler.findValidState.result.error"));
				break;
			default:
				throw new AssertionError("Unhandled find valid state result: " + cmd.getResult());
		}
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
