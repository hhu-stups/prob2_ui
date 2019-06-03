package de.prob2.ui.animation.symbolic.testcasegeneration;


import de.prob2.ui.symbolic.SymbolicExecutionType;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class TestCaseGenerationFormulaExtractor {

    public String extractMCDCFormula(String level) {
        return "MCDC:" + level;
    }

    public String extractMCDCDescription(String depth) {
        return "DEPTH: " + depth + ", " + SymbolicExecutionType.MCDC.getName();
    }

    public String extractOperationCoverageFormula(List<String> operations) {
        return "OPERATION:" + String.join(",", operations);
    }

    public String extractOperationCoverageDescription(String depth) {
        return "DEPTH: " + depth + ", " + SymbolicExecutionType.COVERED_OPERATIONS.getName();
    }

    public String extractDepth(String description) {
        return description.split(",")[0].split(":")[1].replaceAll(" ","");
    }

    public String extractLevel(String formula) {
        return formula.split(":")[1].replaceAll(" ", "");
    }

    public List<String> extractOperations(String formula) {
        return Arrays.asList(formula.split(":")[1].replaceAll(" ","").split(","));
    }
}
