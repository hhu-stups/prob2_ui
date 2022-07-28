package de.prob2.ui.animation.symbolic;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.NoTraceFoundException;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

@Singleton
public class SymbolicAnimationResultHandler implements ISymbolicResultHandler<SymbolicAnimationItem> {
	private final CurrentTrace currentTrace;
	
	@Inject
	public SymbolicAnimationResultHandler(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public void handleFindValidState(SymbolicAnimationItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.getExamples().clear();
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			showCheckingResult(item, Checked.SUCCESS, "animation.symbolic.resultHandler.findValidState.result.found");
			item.getExamples().add(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, Checked.FAIL, "animation.symbolic.resultHandler.findValidState.result.notFound");
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, Checked.INTERRUPTED, "animation.symbolic.resultHandler.findValidState.result.interrupted");
		} else {
			showCheckingResult(item, Checked.PARSE_ERROR, "animation.symbolic.resultHandler.findValidState.result.error");
		}
	}
	
	public void handleSequence(SymbolicAnimationItem item, ConstraintBasedSequenceCheckCommand cmd) {
		ConstraintBasedSequenceCheckCommand.ResultType result = cmd.getResult();
		item.getExamples().clear();
		switch(result) {
			case PATH_FOUND:
				showCheckingResult(item, Checked.SUCCESS, "animation.symbolic.resultHandler.sequence.result.found");
				item.getExamples().add(cmd.getTrace());
				break;
			case NO_PATH_FOUND:
				showCheckingResult(item, Checked.FAIL, "animation.symbolic.resultHandler.sequence.result.notFound");
				break;
			case TIMEOUT: 
				showCheckingResult(item, Checked.INTERRUPTED, "animation.symbolic.resultHandler.sequence.result.timeout");
				break;
			case INTERRUPTED: 
				showCheckingResult(item, Checked.INTERRUPTED, "animation.symbolic.resultHandler.sequence.result.interrupted");
				break;
			case ERROR:
				showCheckingResult(item, Checked.PARSE_ERROR, "animation.symbolic.resultHandler.sequence.result.error");
				break;
			default:
				break;
		}
	}
	
	private void showCheckingResult(SymbolicAnimationItem item, Checked checked, String headerKey, String msgKey, Object... msgParams) {
		item.setResultItem(new CheckingResultItem(checked, headerKey, msgKey, msgParams));
	}
	
	private void showCheckingResult(SymbolicAnimationItem item, Checked checked, String msgKey) {
		showCheckingResult(item, checked, msgKey, msgKey);
	}
	
	@Override
	public void handleFormulaResult(SymbolicAnimationItem item, Object result) {
		CheckingResultItem resultItem = handleFormulaResult(result);
		item.setResultItem(resultItem);
	}
	
	public CheckingResultItem handleFormulaResult(Object result) {
		CheckingResultItem resultItem = null;
		if(result instanceof ModelCheckOk) {
			resultItem = new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.result.succeeded.header",
					"animation.symbolic.result.succeeded.message");
		} else if(result instanceof ProBError || result instanceof CliError || result instanceof CheckError || result instanceof EvaluationException) {
			resultItem = new CheckingResultItem(Checked.PARSE_ERROR, "common.result.couldNotParseFormula.header",
					"common.result.message", result);
		} else if(result instanceof NoTraceFoundException || result instanceof NotYetFinished || result instanceof CheckInterrupted) {
			resultItem = new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header",
					"common.result.message", ((IModelCheckingResult) result).getMessage());
		}
		return resultItem;
	}

	@Override
	public void handleFormulaResult(SymbolicAnimationItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicAnimationType.FIND_VALID_STATE) {
			handleFindValidState(item, (FindStateCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicAnimationType.SEQUENCE) {
			handleSequence(item, (ConstraintBasedSequenceCheckCommand) cmd);
		}
	}
	

}
