package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;
import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorMCDCSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob2.ui.prob2fx.CurrentTrace;

import java.util.ArrayList;
import java.util.List;

public class TestCaseGeneratorCreator {

	private final CurrentTrace currentTrace;
	
	@Inject
	private TestCaseGeneratorCreator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public ConstraintBasedTestCaseGenerator getTestCaseGenerator(TestCaseGenerationItem item) {
		if(item.getType() == TestCaseGenerationType.MCDC) {
			return getMCDCTestCaseGenerator(item);
		} else if(item.getType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			return getCoveredOperationsTestCaseGenerator(item);
		}
		throw new RuntimeException("Unknown type: " + item.getType());
	}
	
	private ConstraintBasedTestCaseGenerator getMCDCTestCaseGenerator(TestCaseGenerationItem item) {
		int depth = item.getMaxDepth();
		int level = item.getMcdcLevel();
		return new ConstraintBasedTestCaseGenerator(currentTrace.getStateSpace(), new TestCaseGeneratorMCDCSettings(depth - 1, level), new ArrayList<>());
	}
	
	private ConstraintBasedTestCaseGenerator getCoveredOperationsTestCaseGenerator(TestCaseGenerationItem item) {
		int depth = item.getMaxDepth();
		final List<String> operations = item.getCoverageOperations();
		return new ConstraintBasedTestCaseGenerator(currentTrace.getStateSpace(), new TestCaseGeneratorOperationCoverageSettings(depth - 1, operations), new ArrayList<>());
	}
	
}
