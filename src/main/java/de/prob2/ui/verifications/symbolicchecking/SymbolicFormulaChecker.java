package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

@Singleton
public class SymbolicFormulaChecker extends SymbolicExecutor {	
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicCheckingResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.items = new ArrayList<>();
	}
	
	public void updateMachine(Machine machine) {
		this.items = machine.getSymbolicCheckingFormulas();
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		injector.getInstance(SymbolicCheckingView.class).refresh();
	}
	
	@Override
	protected void updateTrace(SymbolicFormulaItem item) {
		List<Trace> counterExamples = ((SymbolicCheckingFormulaItem) item).getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}

}
