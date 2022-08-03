package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicFormulaChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicFormulaChecker.class);
	
	protected final CurrentTrace currentTrace;
	protected final SymbolicCheckingResultHandler resultHandler;
	private final CliTaskExecutor cliExecutor;
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final SymbolicCheckingResultHandler resultHandler, final CliTaskExecutor cliExecutor) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.cliExecutor = cliExecutor;
	}
	
	private void updateTrace(SymbolicCheckingFormulaItem item) {
		List<Trace> counterExamples = item.getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}

	public void checkItem(SymbolicCheckingFormulaItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
		final CompletableFuture<AbstractCommand> future = cliExecutor.submit(() -> {
			stateSpace.execute(cmd);
			return cmd;
		});
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
	
	public void checkItem(IModelCheckJob checker, SymbolicCheckingFormulaItem item, boolean checkAll) {
		final CompletableFuture<IModelCheckingResult> future = cliExecutor.submit(checker);
		future.whenComplete((r, e) -> {
			if (e == null) {
				resultHandler.handleFormulaResult(item, r);
			} else {
				LOGGER.error("Exception during symbolic checking", e);
				resultHandler.handleFormulaResult(item, e);
			}
			if(!checkAll) {
				updateTrace(item);
			}
		});
	}
}
