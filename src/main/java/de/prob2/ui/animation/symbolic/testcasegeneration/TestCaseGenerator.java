package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

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
	
	private final CurrentProject currentProject;
	
	private final TestCaseGenerationResultHandler resultHandler;
	
	private final ListProperty<Thread> currentJobThreads;

	@Inject
	public TestCaseGenerator(final CurrentTrace currentTrace, final CurrentProject currentProject, final TestCaseGenerationResultHandler resultHandler, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public void generateTestCases(TestCaseGenerationItem item, ConstraintBasedTestCaseGenerator testCaseGenerator) {
		final TestCaseGenerationItem currentItem = getItemIfAlreadyExists(item);
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
				resultHandler.handleTestCaseGenerationResult(currentItem, finalResult);
			});
			currentJobThreads.remove(Thread.currentThread());
		}, "Test Case Generation Thread");
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
	
	private TestCaseGenerationItem getItemIfAlreadyExists(TestCaseGenerationItem item) {
		return currentProject.getCurrentMachine().getTestCases()
			.stream()
			.filter(item::settingsEqual)
			.findAny()
			.orElse(item);
	}

	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}
}
