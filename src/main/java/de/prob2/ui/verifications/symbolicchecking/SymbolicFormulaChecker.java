package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.IModelCheckJob;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

@Singleton
public class SymbolicFormulaChecker extends SymbolicExecutor {

	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;
	
	@Inject
	public SymbolicFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicCheckingResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
	}
	
	public void executeCheckingItem(IModelCheckJob checker, String code, SymbolicExecutionType type, boolean checkAll) {
		Machine currentMachine = currentProject.getCurrentMachine();
		currentMachine.getSymbolicCheckingFormulas()
			.stream()
			.filter(current -> current.getCode().equals(code) && current.getType().equals(type))
			.findFirst()
			.ifPresent(item -> checkItem(checker, item, checkAll));
	}
		
	public void updateMachine(Machine machine) {
		final SymbolicCheckingView symbolicCheckingView = injector.getInstance(SymbolicCheckingView.class);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		symbolicCheckingView.refresh();
	}
	

	@Override
	protected void updateTrace(SymbolicFormulaItem item) {
		List<Trace> counterExamples = ((SymbolicCheckingFormulaItem) item).getCounterExamples();
		if(!counterExamples.isEmpty()) {
			currentTrace.set(counterExamples.get(0));
		}
	}

}
