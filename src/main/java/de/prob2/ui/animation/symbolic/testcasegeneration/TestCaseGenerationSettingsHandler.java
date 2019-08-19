package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.symbolic.SymbolicChoosingStage;
import de.prob2.ui.symbolic.SymbolicGUIType;

public class TestCaseGenerationSettingsHandler {

	public boolean isValid(SymbolicChoosingStage<SymbolicAnimationFormulaItem> choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		boolean valid = true;
		if(choosingStage.getGUIType() == SymbolicGUIType.MCDC) {
			return checkMCDCSettings(mcdcInputView.getLevel(), mcdcInputView.getDepth());
		} else if(choosingStage.getGUIType() == SymbolicGUIType.OPERATIONS) {
			return checkOperationCoverageSettings(operationCoverageInputView.getOperations(), operationCoverageInputView.getDepth());
		}
		return valid;
	}
	
	public boolean checkMCDCSettings(String level, String depth) {
		return checkInteger(level) && checkInteger(depth);
	}
	
	public boolean checkOperationCoverageSettings(List<String> operations, String depth) {
		return !operations.isEmpty() && checkInteger(depth);
	}
	
	public boolean checkInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public Map<String, Object> extractAdditionalInformation(SymbolicChoosingStage<SymbolicAnimationFormulaItem> choosingStage, MCDCInputView mcdcInputView, OperationCoverageInputView operationCoverageInputView) {
		Map<String, Object> additionalInformation = new HashMap<>();
		if(choosingStage.getGUIType() == SymbolicGUIType.MCDC) {
			String level = mcdcInputView.getLevel();
			String depth = mcdcInputView.getDepth();
			additionalInformation.put("maxDepth", depth);
			additionalInformation.put("level", level);
		} else if(choosingStage.getGUIType() == SymbolicGUIType.OPERATIONS) {
			List<String> operations = operationCoverageInputView.getOperations();
			String depth = operationCoverageInputView.getDepth();
			additionalInformation.put("maxDepth", depth);
			additionalInformation.put("operations", operations);
		}
		return additionalInformation;
	}
	
}
