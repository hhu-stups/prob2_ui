package de.prob2.ui.verifications.cbc;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.TraceResult;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class CBCAssertionCheckingItem extends SymbolicCheckingFormulaItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCAssertionCheckingItem.class);
	
	protected CBCAssertionCheckingItem(String id) {
		super(id);
	}
	
	protected abstract ConstraintBasedAssertionCheckCommand.CheckingType getAssertionCheckingType();
	
	@Override
	public void execute(ExecutionContext context) {
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(this.getAssertionCheckingType(), context.stateSpace());
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Symbolic checking interrupted by user", exc);
			this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED));
		}
		
		switch (cmd.getResult()) {
			case NO_COUNTER_EXAMPLE_EXISTS:
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.cbc.assertionChecking.result.noCounterExampleExists"));
				break;
			case NO_COUNTER_EXAMPLE_FOUND:
				this.setResult(new CheckingResult(CheckingStatus.SUCCESS, "verifications.cbc.assertionChecking.result.noCounterExampleFound"));
				break;
			case COUNTER_EXAMPLE:
				this.setResult(new TraceResult(CheckingStatus.FAIL, cmd.getTrace(context.stateSpace()), "verifications.cbc.assertionChecking.result.counterExampleFound"));
				break;
			case INTERRUPTED:
				this.setResult(new CheckingResult(CheckingStatus.INTERRUPTED));
				break;
			default:
				throw new AssertionError("Unhandled CBC assertion checking result: " + cmd.getResult());
		}
	}
}
