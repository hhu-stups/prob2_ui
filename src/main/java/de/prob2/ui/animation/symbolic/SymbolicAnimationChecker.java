package de.prob2.ui.animation.symbolic;

import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutor;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor<SymbolicAnimationItem> {

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, resultHandler, injector);
	}

	@Override
	protected void updateTrace(SymbolicAnimationItem item) {
		List<Trace> examples = item.getExamples();
		if(!examples.isEmpty()) {
			currentTrace.set(examples.get(0));
		}
	}

	
}
