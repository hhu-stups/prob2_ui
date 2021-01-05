package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public boolean checkMCDCSettings(String level, String depth) {
		return checkInteger(level) && checkInteger(depth) && Integer.parseInt(level) >= 0 && Integer.parseInt(depth) > 0;
	}
	
	public boolean checkOperationCoverageSettings(List<String> operations, String depth) {
		return !operations.isEmpty() && checkInteger(depth) && Integer.parseInt(depth) > 0;
	}
	
	public boolean checkInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public Map<String, Object> extractAdditionalInformation(TestCaseGenerationChoosingStage choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		Map<String, Object> additionalInformation = new HashMap<>();
		if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			String level = mcdcInputView.getLevel();
			additionalInformation.put("level", level);
		} else if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.COVERED_OPERATIONS) {
			List<String> operations = operationCoverageInputView.getOperations();
			additionalInformation.put("operations", operations);
		}
		return additionalInformation;
	}
	
	public String extractDepth(TestCaseGenerationChoosingStage choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		if(choosingStage.getTestCaseGenerationType() == TestCaseGenerationType.MCDC) {
			return mcdcInputView.getDepth();
		} else {
			return operationCoverageInputView.getDepth();
		}
	}
	
}
