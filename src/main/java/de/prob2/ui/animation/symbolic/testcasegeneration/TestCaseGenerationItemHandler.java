package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.StateSpace;

@Singleton
public class TestCaseGenerationItemHandler {
	private final TestCaseGenerator testCaseGenerator;
	
	@Inject
	private TestCaseGenerationItemHandler(final TestCaseGenerator testCaseGenerator) {
		this.testCaseGenerator = testCaseGenerator;
	}

	public void generateTestCases(TestCaseGenerationItem item, StateSpace stateSpace) {
		AbstractModel model = stateSpace.getModel();
		if(!(model instanceof ClassicalBModel) && !(model instanceof EventBModel)) {
			return;
		}
		ConstraintBasedTestCaseGenerator cbTestCaseGenerator = new ConstraintBasedTestCaseGenerator(stateSpace, item.getTestCaseGeneratorSettings(), new ArrayList<>());
		testCaseGenerator.generateTestCases(item, cbTestCaseGenerator);
	}
}
