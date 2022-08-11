package de.prob2.ui.animation.symbolic;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.statespace.StateSpace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

@Singleton
public class SymbolicAnimationResultHandler {
	@Inject
	public SymbolicAnimationResultHandler() {}
	
	public void handleFindValidState(SymbolicAnimationItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.getExamples().clear();
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.resultHandler.findValidState.result.found"));
			item.getExamples().add(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.symbolic.resultHandler.findValidState.result.notFound"));
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.symbolic.resultHandler.findValidState.result.interrupted"));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.PARSE_ERROR, "animation.symbolic.resultHandler.findValidState.result.error"));
		}
	}
	
	public void handleSequence(SymbolicAnimationItem item, ConstraintBasedSequenceCheckCommand cmd) {
		ConstraintBasedSequenceCheckCommand.ResultType result = cmd.getResult();
		item.getExamples().clear();
		switch(result) {
			case PATH_FOUND:
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.resultHandler.sequence.result.found"));
				item.getExamples().add(cmd.getTrace());
				break;
			case NO_PATH_FOUND:
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.symbolic.resultHandler.sequence.result.notFound"));
				break;
			case TIMEOUT:
				item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.symbolic.resultHandler.sequence.result.timeout"));
				break;
			case INTERRUPTED:
				item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.symbolic.resultHandler.sequence.result.interrupted"));
				break;
			case ERROR:
				item.setResultItem(new CheckingResultItem(Checked.PARSE_ERROR, "animation.symbolic.resultHandler.sequence.result.error"));
				break;
			default:
				break;
		}
	}
	
	public void handleFormulaException(SymbolicAnimationItem item, Throwable result) {
		item.setResultItem(new CheckingResultItem(Checked.PARSE_ERROR, "common.result.message", result));
	}
}
