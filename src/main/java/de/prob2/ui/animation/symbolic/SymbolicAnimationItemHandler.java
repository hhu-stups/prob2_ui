package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;

import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.statespace.StateSpace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SymbolicAnimationItemHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicAnimationItemHandler.class);

	private SymbolicAnimationItemHandler() {
		throw new AssertionError("Utility class");
	}

	private static void handleSequence(SymbolicAnimationItem item, StateSpace stateSpace) {
		List<String> events = Arrays.asList(item.getCode().replace(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(stateSpace, events, new ClassicalB("1=1"));
		stateSpace.execute(cmd);
		ConstraintBasedSequenceCheckCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		switch(result) {
			case PATH_FOUND:
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.resultHandler.sequence.result.found"));
				item.setExample(cmd.getTrace());
				break;
			case NO_PATH_FOUND:
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.symbolic.resultHandler.sequence.result.notFound"));
				break;
			case TIMEOUT:
				item.setResultItem(new CheckingResultItem(Checked.TIMEOUT, "animation.symbolic.resultHandler.sequence.result.timeout"));
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

	private static void findValidState(SymbolicAnimationItem item, StateSpace stateSpace) {
		FindStateCommand cmd = new FindStateCommand(stateSpace, new ClassicalB(item.getCode()), true);
		stateSpace.execute(cmd);
		FindStateCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.resultHandler.findValidState.result.found"));
			item.setExample(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.symbolic.resultHandler.findValidState.result.notFound"));
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.symbolic.resultHandler.findValidState.result.interrupted"));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.PARSE_ERROR, "animation.symbolic.resultHandler.findValidState.result.error"));
		}
	}

	private static void executeItemInternal(final SymbolicAnimationItem item, final StateSpace stateSpace) {
		switch(item.getType()) {
			case SEQUENCE:
				handleSequence(item, stateSpace);
				break;
			case FIND_VALID_STATE:
				findValidState(item, stateSpace);
				break;
			default:
				throw new AssertionError("Unhandled symbolic animation type: " + item.getType());
		}
	}

	public static void executeItem(final SymbolicAnimationItem item, final StateSpace stateSpace) {
		try {
			executeItemInternal(item, stateSpace);
		} catch (RuntimeException exc) {
			LOGGER.error("Exception during symbolic animation", exc);
			item.setResultItem(new CheckingResultItem(Checked.PARSE_ERROR, "common.result.message", exc));
		}
	}
}
