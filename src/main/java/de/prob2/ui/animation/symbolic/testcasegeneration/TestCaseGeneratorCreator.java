package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorMCDCSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob2.ui.prob2fx.CurrentTrace;

public class TestCaseGeneratorCreator {

	private final CurrentTrace currentTrace;
	
	@Inject
	private TestCaseGeneratorCreator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public ConstraintBasedTestCaseGenerator getTestCaseGenerator(ClassicalBModel bModel, TestCaseGenerationItem item) {
		if(item.getType() == TestCaseGenerationType.MCDC) {
			return getMCDCTestCaseGenerator(bModel, item);
		} else if(item.getType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			return getCoveredOperationsTestCaseGenerator(bModel, item);
		}
		throw new RuntimeException("Unknown type: " + item.getType());
	}
	
	private ConstraintBasedTestCaseGenerator getMCDCTestCaseGenerator(ClassicalBModel bModel, TestCaseGenerationItem item) {
		int depth = item.getMaxDepth();
		int level = (int) Double.parseDouble(item.getAdditionalInformation("level").toString());
		return new ConstraintBasedTestCaseGenerator(bModel, currentTrace.getStateSpace(), new TestCaseGeneratorMCDCSettings(depth, level), new ArrayList<>());
	}
	
	private ConstraintBasedTestCaseGenerator getCoveredOperationsTestCaseGenerator(ClassicalBModel bModel, TestCaseGenerationItem item) {
		int depth = item.getMaxDepth();
		@SuppressWarnings("unchecked")
		List<String> operations = (List<String>) item.getAdditionalInformation("operations");
		return new ConstraintBasedTestCaseGenerator(bModel, currentTrace.getStateSpace(), new TestCaseGeneratorOperationCoverageSettings(depth, operations), new ArrayList<>());
	}
	
}
