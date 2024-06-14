package de.prob2.ui.verifications.cbc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CBCRefinementCheckingItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCRefinementCheckingItem.class);
	
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
	
	@Override
	public void execute(ExecutionContext context) {
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("CBC refinement checking interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "common.result.message", exc.getMessage()));
			return;
		}

		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if (result == null) {
			this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.cbc.refinementChecking.result.notARefinementMachine.message"));
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.cbc.refinementChecking.result.noViolationFound", msg));
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			this.setResult(new CheckingResult(CheckingStatus.FAIL, "verifications.cbc.refinementChecking.result.violationFound", msg));
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.INTERRUPTED) {
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED, "verifications.cbc.refinementChecking.result.interrupted", msg));
		} else {
			throw new AssertionError("Unhandled CBC refinement checking result: " + result);
		}
	}
}
