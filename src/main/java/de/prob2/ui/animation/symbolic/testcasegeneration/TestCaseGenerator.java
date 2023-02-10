package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestCaseGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseGenerator.class);
	
	private final CurrentTrace currentTrace;
	
	private final ListProperty<Thread> currentJobThreads;

	@Inject
	public TestCaseGenerator(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	private static void handleResult(TestCaseGenerationItem item, TestCaseGeneratorResult result) {
		item.setResult(result);
		item.getExamples().clear();
		
		List<Trace> traces = new ArrayList<>();
		for (final TestTrace trace : result.getTestTraces()) {
			if (trace.getTrace() != null) {
				traces.add(trace.getTrace());
			}
		}
		
		if(result.isInterrupted()) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "animation.resultHandler.testcasegeneration.result.interrupted"));
		} else if(traces.isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound"));
		} else if(!result.getUncoveredTargets().isEmpty()) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notAllGenerated"));
		} else {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "animation.resultHandler.testcasegeneration.result.found"));
		}
		item.getExamples().addAll(traces);
	}

	public void generateTestCases(TestCaseGenerationItem item, ConstraintBasedTestCaseGenerator testCaseGenerator) {
		Thread checkingThread = new Thread(() -> {
			try {
				final TestCaseGeneratorResult result = testCaseGenerator.generateTestCases();
				Platform.runLater(() -> handleResult(item, result));
			} catch (RuntimeException e) {
				LOGGER.error("Exception during generating test cases", e);
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "animation.resultHandler.testcasegeneration.result.notFound"));
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Test Case Generation Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void generateTestCases(TestCaseGenerationItem item, StateSpace stateSpace) {
		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = new ConstraintBasedTestCaseGenerator(stateSpace, item.getTestCaseGeneratorSettings(), new ArrayList<>());
		generateTestCases(item, cbTestCaseGenerator);
	}
	
	public void interrupt() {
		List<Thread> removedThreads = new ArrayList<>();
		for (Thread thread : currentJobThreads) {
			thread.interrupt();
			removedThreads.add(thread);
		}
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
	}
	
	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}
}
