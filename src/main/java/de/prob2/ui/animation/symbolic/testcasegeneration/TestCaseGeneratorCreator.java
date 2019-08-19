package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorMCDCSettings;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorOperationCoverageSettings;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.SymbolicExecutionType;

public class TestCaseGeneratorCreator {

	private final CurrentTrace currentTrace;
	
	@Inject
	private TestCaseGeneratorCreator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	public ConstraintBasedTestCaseGenerator getTestCaseGenerator(ClassicalBModel bModel, SymbolicAnimationFormulaItem item) {
		if(item.getType() == SymbolicExecutionType.MCDC) {
			return getMCDCTestCaseGenerator(bModel, item);
		} else if(item.getType() == SymbolicExecutionType.COVERED_OPERATIONS) {
			return getCoveredOperationsTestCaseGenerator(bModel, item);
		}
		return null;
	}
	
	private ConstraintBasedTestCaseGenerator getMCDCTestCaseGenerator(ClassicalBModel bModel, SymbolicAnimationFormulaItem item) {
		int depth = (int) Double.parseDouble(item.getAdditionalInformation("maxDepth").toString());
		int level = (int) Double.parseDouble(item.getAdditionalInformation("level").toString());
		return new ConstraintBasedTestCaseGenerator(bModel, currentTrace.getStateSpace(), new TestCaseGeneratorMCDCSettings(depth, level), new ArrayList<>());
	}
	
	private ConstraintBasedTestCaseGenerator getCoveredOperationsTestCaseGenerator(ClassicalBModel bModel, SymbolicAnimationFormulaItem item) {
		int depth = (int) Double.parseDouble(item.getAdditionalInformation("maxDepth").toString());
		@SuppressWarnings("unchecked")
		List<String> operations = (List<String>) item.getAdditionalInformation("operations");
		return new ConstraintBasedTestCaseGenerator(bModel, currentTrace.getStateSpace(), new TestCaseGeneratorOperationCoverageSettings(depth, operations), new ArrayList<>());
	}
	
}
