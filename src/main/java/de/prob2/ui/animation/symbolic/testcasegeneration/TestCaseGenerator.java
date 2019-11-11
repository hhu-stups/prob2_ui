package de.prob2.ui.animation.symbolic.testcasegeneration;


import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TestCaseGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseGenerator.class);
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final Injector injector;

	private final TestCaseGenerationResultHandler resultHandler;
	
	private final ListProperty<Thread> currentJobThreads;

	@Inject
	public TestCaseGenerator(final CurrentTrace currentTrace, final CurrentProject currentProject, final TestCaseGenerationResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.injector = injector;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}

	public void generateTestCases(TestCaseGenerationItem item, ConstraintBasedTestCaseGenerator testCaseGenerator, boolean checkAll) {
		final TestCaseGenerationItem currentItem = (TestCaseGenerationItem) getItemIfAlreadyExists(item);
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
				resultHandler.handleTestCaseGenerationResult(currentItem, finalResult, checkAll);
				updateMachine(currentProject.getCurrentMachine());
			});
			currentJobThreads.remove(Thread.currentThread());
		}, "Symbolic Formula Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
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
	
	public void updateMachine(Machine machine) {
		injector.getInstance(TestCaseGenerationView.class).refresh();
	}
	
	private TestCaseGenerationItem getItemIfAlreadyExists(TestCaseGenerationItem item) {
		List<TestCaseGenerationItem> formulas = getItems();
		int index = formulas.indexOf(item);
		if(index > -1) {
			item = formulas.get(index);
		}
		return item;
	}
	
	private List<TestCaseGenerationItem> getItems() {
		return currentProject.getCurrentMachine().getTestCases();
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

	
}
