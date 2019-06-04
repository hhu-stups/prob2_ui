package de.prob2.ui.animation.symbolic.testcasegeneration;


import de.prob2.ui.symbolic.SymbolicExecutionType;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class TestCaseGenerationFormulaExtractor {

    public String extractRawFormula(String formula) {
        return formula.replaceAll(" ", "").split("/")[0];
    }

    public String extractMCDCFormula(String level, String depth) {
        return "MCDC:" + level + "/" + "DEPTH:" + depth;
    }

    public String extractOperationCoverageFormula(List<String> operations, String depth) {
        return "OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + depth;
    }

    public String extractDepth(String formula) {
        String[] splittedString = formula.replaceAll(" ", "").split("/");
        return splittedString[1].split(":")[1];
    }

    public String extractLevel(String formula) {
        String[] splittedString = formula.replaceAll(" ", "").split("/");
        return splittedString[0].split(":")[1];
    }

    public List<String> extractOperations(String formula) {
        String[] splittedString = formula.replaceAll(" ", "").split("/");
        return Arrays.asList(splittedString[1].split(":")[1].split(","));
    }
}
