package de.prob2.ui.animation.symbolic;


import java.util.ArrayList;

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
public class SymbolicAnimationChecker extends SymbolicExecutor {

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.items = new ArrayList<>();
	}
	
	public void updateMachine(Machine machine) {
		this.items = machine.getSymbolicAnimationFormulas();
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC_ANIMATION);
		injector.getInstance(SymbolicAnimationView.class).refresh();
	}
	
	@Override
	protected void updateTrace(SymbolicFormulaItem item) {
		Trace example = ((SymbolicAnimationFormulaItem) item).getExample();
		if(example != null) {
			currentTrace.set(example);
		}
	}

	
}
