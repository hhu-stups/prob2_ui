package de.prob2.ui.animation.symbolic.testcasegeneration;

public class TestCaseGenerationSettingsHandler {

	public boolean isValid(TestCaseGenerationChoosingStage choosingStage, OperationCoverageInputView operationCoverageInputView) {
		if (choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			return !operationCoverageInputView.getOperations().isEmpty();
		} else {
			return true;
		}
	}
}
