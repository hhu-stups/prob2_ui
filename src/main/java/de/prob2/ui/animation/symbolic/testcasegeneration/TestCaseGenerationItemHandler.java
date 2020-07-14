package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

@Singleton
public class TestCaseGenerationItemHandler {

	private final CurrentTrace currentTrace;

	private final TestCaseGenerator testCaseGenerator;
	
	private final TestCaseGeneratorCreator testCaseGeneratorCreator;

	private final CurrentProject currentProject;

	@Inject
	private TestCaseGenerationItemHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
										   final TestCaseGenerator testCaseGenerator,
										   final TestCaseGeneratorCreator testCaseGeneratorCreator,
										   final TestCaseGenerationResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.testCaseGenerator = testCaseGenerator;
		this.testCaseGeneratorCreator = testCaseGeneratorCreator;
	}

	public void addItem(String name, TestCaseGenerationType type, boolean checking) {
		TestCaseGenerationItem item = new TestCaseGenerationItem(name, type);
		addItem(item,checking);
	}

	public void addItem(int depth, int level, boolean checking) {
		TestCaseGenerationItem item = new TestCaseGenerationItem(depth, level);
		addItem(item,checking);
	}

	public void addItem(int depth, List<String> operations, boolean checking) {
		TestCaseGenerationItem item = new TestCaseGenerationItem(depth, operations);
		addItem(item,checking);
	}

	public void addItem(TestCaseGenerationItem item, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getTestCases().contains(item)) {
				currentMachine.addTestCase(item);
			}
		}
	}

	public void generateTestCases(TestCaseGenerationItem item, boolean checkAll) {
		AbstractModel model = currentTrace.getModel();
		if(!(model instanceof ClassicalBModel)) {
			return;
		}
		ClassicalBModel bModel = (ClassicalBModel) model;
		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = testCaseGeneratorCreator.getTestCaseGenerator(bModel, item);
		testCaseGenerator.generateTestCases(item, cbTestCaseGenerator, checkAll);
	}


	public void handleItem(TestCaseGenerationItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		TestCaseGenerationType type = item.getType();
		switch(type) {
			case MCDC:
				generateTestCases(item, checkAll);
				break;
			case COVERED_OPERATIONS:
				generateTestCases(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getTestCases().forEach(item -> handleItem(item, true));
	}
	
}
