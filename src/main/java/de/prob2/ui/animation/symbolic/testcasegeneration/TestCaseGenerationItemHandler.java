package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractCheckableItem;

@Singleton
public class TestCaseGenerationItemHandler {

	private final CurrentTrace currentTrace;

	private final TestCaseGenerator testCaseGenerator;
	
	@Inject
	private TestCaseGenerationItemHandler(final CurrentTrace currentTrace, final TestCaseGenerator testCaseGenerator) {
		this.currentTrace = currentTrace;
		this.testCaseGenerator = testCaseGenerator;
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
		generateTestCases(item);
	}
	
	public void handleMachine(Machine machine) {
		machine.getTestCases().stream()
			.filter(AbstractCheckableItem::selected)
			.forEach(this::handleItem);
	}
	
}
