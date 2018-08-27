package de.prob2.ui.animation.symbolic;


import javax.inject.Inject;


import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.IModelCheckJob;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor {
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;

	
	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicExecutionType type, boolean checkAll) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getSymbolicAnimationFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item, checkAll));
	}
	
	@Override
	protected void updateTrace(SymbolicFormulaItem item) {
		Trace example = ((SymbolicAnimationFormulaItem) item).getExample();
		if(example != null) {
			currentTrace.set(example);
		}
	}
		
	public void updateMachine(Machine machine) {
		final SymbolicAnimationView symbolicAnimationView = injector.getInstance(SymbolicAnimationView.class);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		symbolicAnimationView.refresh();
	}
	
}
