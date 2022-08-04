package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

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

	private CompletableFuture<SymbolicAnimationItem> checkItem(final SymbolicAnimationItem item, final Runnable task) {
		return cliExecutor.submit(task, item).exceptionally(e -> {
			LOGGER.error("Exception during symbolic animation", e);
			resultHandler.handleFormulaException(item, e);
			return item;
		});
	}

	public CompletableFuture<SymbolicAnimationItem> handleSequence(SymbolicAnimationItem item) {
		List<String> events = Arrays.asList(item.getCode().replace(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(currentTrace.getStateSpace(), events, new ClassicalB("1=1", FormulaExpand.EXPAND));
		return checkItem(item, () -> {
			currentTrace.getStateSpace().execute(cmd);
			resultHandler.handleSequence(item, cmd);
		});
	}

	public CompletableFuture<SymbolicAnimationItem> findValidState(SymbolicAnimationItem item) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new ClassicalB(item.getCode(), FormulaExpand.EXPAND), true);
		return checkItem(item, () -> {
			stateSpace.execute(cmd);
			resultHandler.handleFindValidState(item, cmd, stateSpace);
		});
	}

	@Override
	public CompletableFuture<SymbolicAnimationItem> handleItemNoninteractive(final SymbolicAnimationItem item) {
		switch(item.getType()) {
			case SEQUENCE:
				return handleSequence(item);
			case FIND_VALID_STATE:
				return findValidState(item);
			default:
				throw new AssertionError("Unhandled symbolic animation type: " + item.getType());
		}
	}

	@Override
	public CompletableFuture<SymbolicAnimationItem> handleItem(SymbolicAnimationItem item, boolean checkAll) {
		if(!item.selected()) {
			return CompletableFuture.completedFuture(item);
		}
		return handleItemNoninteractive(item).thenApply(r -> {
			if(!checkAll) {
				List<Trace> examples = item.getExamples();
				if(!examples.isEmpty()) {
					currentTrace.set(examples.get(0));
				}
			}
			return r;
		});
	}
	
	@Override
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
