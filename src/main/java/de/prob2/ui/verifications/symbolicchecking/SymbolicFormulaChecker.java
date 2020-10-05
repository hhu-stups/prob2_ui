package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicItem;

@Singleton
public class SymbolicFormulaChecker extends SymbolicExecutor {	
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final SymbolicCheckingResultHandler resultHandler, final Injector injector) {
		super(currentTrace, resultHandler, injector);
	}
	
	@Override
	protected void updateTrace(SymbolicItem item) {
		List<Trace> counterExamples = ((SymbolicCheckingFormulaItem) item).getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}

}
