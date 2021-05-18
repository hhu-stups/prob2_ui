package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

@Singleton
public class TestCaseGenerationItemHandler {

	private final CurrentTrace currentTrace;

	private final TestCaseGenerator testCaseGenerator;
	
	private final CurrentProject currentProject;

	@Inject
	private TestCaseGenerationItemHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
										   final TestCaseGenerator testCaseGenerator) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.testCaseGenerator = testCaseGenerator;
	}

	public void addItem(int depth, int level) {
		TestCaseGenerationItem item = new MCDCItem(depth, level);
		addItem(item);
	}

	public void addItem(int depth, List<String> operations) {
		TestCaseGenerationItem item = new OperationCoverageItem(depth, operations);
		addItem(item);
	}

	public void addItem(TestCaseGenerationItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(currentMachine.getTestCases().stream().noneMatch(item::settingsEqual)) {
				currentMachine.getTestCases().add(item);
			}
		}
	}

	public void generateTestCases(TestCaseGenerationItem item) {
		AbstractModel model = currentTrace.getModel();
		if(!(model instanceof ClassicalBModel) && !(model instanceof EventBModel)) {
			return;
		}
		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = new ConstraintBasedTestCaseGenerator(currentTrace.getStateSpace(), item.getTestCaseGeneratorSettings(), new ArrayList<>());
		testCaseGenerator.generateTestCases(item, cbTestCaseGenerator);
	}


	public void handleItem(TestCaseGenerationItem item) {
		if(!item.selected()) {
			return;
		}
		generateTestCases(item);
	}
	
	public void handleMachine(Machine machine) {
		machine.getTestCases().forEach(this::handleItem);
	}
	
}
