package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
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
public final class CBCFindSequenceItem extends SymbolicAnimationItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCFindSequenceItem.class);
	
	private final List<String> operationNames;
	
	@JsonCreator
	public CBCFindSequenceItem(
		@JsonProperty("id") String id,
		@JsonProperty("operationNames") List<String> operationNames
	) {
		super(id);
		
		this.operationNames = new ArrayList<>(operationNames);
	}
	
	public List<String> getOperationNames() {
		return Collections.unmodifiableList(this.operationNames);
	}
	
	@Override
	public ValidationTaskType<CBCFindSequenceItem> getTaskType() {
		return BuiltinValidationTaskTypes.CBC_FIND_SEQUENCE;
	}
	
	@Override
	public String getTaskType(I18n i18n) {
		return i18n.translate("animation.type.sequence");
	}
	
	@Override
	public String getTaskDescription(I18n i18n) {
		return String.join(";", this.getOperationNames());
	}
	
	@Override
	public void execute(ExecutionContext context) {
		this.setExample(null);
		
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(context.stateSpace(), getOperationNames(), new ClassicalB("1=1"));
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("CBC find sequence interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}
		
		switch (cmd.getResult()) {
			case PATH_FOUND:
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "animation.symbolic.resultHandler.sequence.result.found"));
				this.setExample(cmd.getTrace());
				break;
			case NO_PATH_FOUND:
				this.setResult(new CheckingResult(CheckingStatus.FAIL, "animation.symbolic.resultHandler.sequence.result.notFound"));
				break;
			case TIMEOUT:
				this.setResult(new CheckingResult(CheckingStatus.TIMEOUT, "animation.symbolic.resultHandler.sequence.result.timeout"));
				break;
			case INTERRUPTED:
				this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "animation.symbolic.resultHandler.sequence.result.interrupted"));
				break;
			case ERROR:
				this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "animation.symbolic.resultHandler.sequence.result.error"));
				break;
			default:
				throw new AssertionError("Unhandled CBC find sequence result: " + cmd.getResult());
		}
	}
	
	@Override
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof CBCFindSequenceItem o
			&& this.getOperationNames().equals(o.getOperationNames());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("operationNames", this.getOperationNames())
			.toString();
	}
}
