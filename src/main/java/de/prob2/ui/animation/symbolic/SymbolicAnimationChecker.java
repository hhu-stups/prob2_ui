package de.prob2.ui.animation.symbolic;


import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;

import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolicAnimationChecker.class);

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
		this.items = new ArrayList<>();
	}

	public void checkItem(SymbolicAnimationFormulaItem item, ConstraintBasedTestCaseGenerator testCaseGenerator) {
		final SymbolicAnimationFormulaItem currentItem = (SymbolicAnimationFormulaItem) getItemIfAlreadyExists(item);
		Thread checkingThread = new Thread(() -> {
			Object result;
			try {
				result = testCaseGenerator.generateTestCases();
			} catch (RuntimeException e) {
				LOGGER.error("Exception during generating test cases", e);
				result = e;
			}
			final Object finalResult = result;
			Platform.runLater(() -> {
				((SymbolicAnimationResultHandler) resultHandler).handleTestCaseGenerationResult(currentItem, finalResult);
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
		List<Trace> examples = ((SymbolicAnimationFormulaItem) item).getExamples();
		if(!examples.isEmpty()) {
			currentTrace.set(examples.get(0));
		}
	}

	
}
