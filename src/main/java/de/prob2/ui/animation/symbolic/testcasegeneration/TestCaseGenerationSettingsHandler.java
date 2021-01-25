package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

public class TestCaseGenerationSettingsHandler {

	public boolean isValid(TestCaseGenerationChoosingStage choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		boolean valid = true;
		if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			return checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth());
		} else if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			return checkOperationCoverageSettings(operationCoverageInputView.getOperations(), operationCoverageInputView.getDepth());
		}
		return valid;
	}
	
	public boolean checkMCDCSettings(int level, int depth) {
		return level >= 0 && depth > 0;
	}
	
	public boolean checkOperationCoverageSettings(List<String> operations, int depth) {
		return !operations.isEmpty() && depth > 0;
	}
	
	public int extractDepth(TestCaseGenerationChoosingStage choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			return mcdcInputView.getDepth();
		} else {
			return operationCoverageInputView.getDepth();
		}
	}
	
}
