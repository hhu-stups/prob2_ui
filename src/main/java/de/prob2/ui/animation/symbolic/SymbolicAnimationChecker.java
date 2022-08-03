package de.prob2.ui.animation.symbolic;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.symbolic.SymbolicExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicAnimationChecker.class);

	private final ISymbolicResultHandler<SymbolicAnimationItem> resultHandler;
	private final CliTaskExecutor cliExecutor;

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final SymbolicAnimationResultHandler resultHandler, final Injector injector, final CliTaskExecutor cliExecutor) {
		super(currentTrace, injector);
		this.resultHandler = resultHandler;
		this.cliExecutor = cliExecutor;
	}

	private void updateTrace(SymbolicAnimationItem item) {
		List<Trace> examples = item.getExamples();
		if(!examples.isEmpty()) {
			currentTrace.set(examples.get(0));
		}
	}

	public void checkItem(SymbolicAnimationItem item, AbstractCommand cmd, final StateSpace stateSpace, boolean checkAll) {
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
}
