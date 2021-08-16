package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutor;

@Singleton
public class SymbolicFormulaChecker extends SymbolicExecutor<SymbolicCheckingFormulaItem> {
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final SymbolicCheckingResultHandler resultHandler, final Injector injector) {
		super(currentTrace, resultHandler, injector);
	}
	
	@Override
	protected void updateTrace(SymbolicCheckingFormulaItem item) {
		List<Trace> counterExamples = item.getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}

}
