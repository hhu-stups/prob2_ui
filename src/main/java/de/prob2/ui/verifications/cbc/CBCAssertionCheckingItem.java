package de.prob2.ui.verifications.cbc;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ExecutionContext;
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
		this.getCounterExamples().clear();
		
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(this.getAssertionCheckingType(), context.stateSpace());
		try {
			context.stateSpace().execute(cmd);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Symbolic checking interrupted by user", exc);
			this.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", exc.getMessage()));
		}
		
		switch (cmd.getResult()) {
			case NO_COUNTER_EXAMPLE_EXISTS:
				this.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleExists"));
				break;
			case NO_COUNTER_EXAMPLE_FOUND:
				this.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.assertionChecking.result.noCounterExampleFound"));
				break;
			case COUNTER_EXAMPLE:
				this.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.symbolicchecking.resultHandler.assertionChecking.result.counterExampleFound"));
				this.getCounterExamples().add(cmd.getTrace(context.stateSpace()));
				break;
			case INTERRUPTED:
				this.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.assertionChecking.result.interrupted"));
				break;
			default:
				throw new AssertionError("Unhandled CBC assertion checking result: " + cmd.getResult());
		}
	}
}
