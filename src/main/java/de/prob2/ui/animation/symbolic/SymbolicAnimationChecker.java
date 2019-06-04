package de.prob2.ui.animation.symbolic;


import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.animation.symbolic.SymbolicAnimationResultHandler;
import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.application.Platform;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor {

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.items = new ArrayList<>();
	}

	public void checkItem(SymbolicAnimationFormulaItem item, ConstraintBasedTestCaseGenerator testCaseGenerator, boolean checkAll) {
		final SymbolicAnimationFormulaItem currentItem = (SymbolicAnimationFormulaItem) getItemIfAlreadyExists(item);
		Thread checkingThread = new Thread(() -> {
			TestCaseGeneratorResult result = testCaseGenerator.generateTestCases();
			Platform.runLater(() -> {
				((SymbolicAnimationResultHandler) resultHandler).handleTestCaseGenerationResult(currentItem, result);
				updateMachine(currentProject.getCurrentMachine());
			});
			currentJobThreads.remove(Thread.currentThread());
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
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
