package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicAnimationItemHandler implements SymbolicFormulaHandler<SymbolicAnimationItem> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicAnimationItemHandler.class);

	private final CurrentTrace currentTrace;
	private final SymbolicAnimationResultHandler resultHandler;
	private final CliTaskExecutor cliExecutor;

	@Inject
	private SymbolicAnimationItemHandler(final CurrentTrace currentTrace, final SymbolicAnimationResultHandler resultHandler, final CliTaskExecutor cliExecutor) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.cliExecutor = cliExecutor;
	}

	@Override
	public List<SymbolicAnimationItem> getItems(final Machine machine) {
		return machine.getSymbolicAnimationFormulas();
	}

	private void updateTrace(SymbolicAnimationItem item) {
		List<Trace> examples = item.getExamples();
		if(!examples.isEmpty()) {
			currentTrace.set(examples.get(0));
		}
	}

	private void checkItem(SymbolicAnimationItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final CompletableFuture<AbstractCommand> future = cliExecutor.submit(() -> {
			stateSpace.execute(cmd);
			return cmd;
		});
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic animation", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}

	public void handleSequence(SymbolicAnimationItem item, boolean checkAll) {
		List<String> events = Arrays.asList(item.getCode().replace(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(currentTrace.getStateSpace(), events, new ClassicalB("1=1", FormulaExpand.EXPAND));
		checkItem(item, cmd, currentTrace.getStateSpace(), checkAll);
	}

	public void findValidState(SymbolicAnimationItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new ClassicalB(item.getCode(), FormulaExpand.EXPAND), true);
		checkItem(item, cmd, stateSpace, checkAll);
	}

	@Override
	public void handleItem(SymbolicAnimationItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		switch(item.getType()) {
			case SEQUENCE:
				handleSequence(item, checkAll);
				break;
			case FIND_VALID_STATE:
				findValidState(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	@Override
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
