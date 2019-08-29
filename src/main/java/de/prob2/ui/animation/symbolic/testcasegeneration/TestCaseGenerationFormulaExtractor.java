package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Arrays;
import java.util.List;

public class TestCaseGenerationFormulaExtractor {

    private TestCaseGenerationFormulaExtractor(){}

    public static int extractDepth(String formula) {
        String[] splittedStringBySlash = formula.replace(" ", "").split("/");
        String[] splittedStringByColon = splittedStringBySlash[1].split(":");
        return Integer.parseInt(splittedStringByColon[1]);
    }

    public static int extractLevel(String formula) {
        String[] splittedStringBySlash = formula.replace(" ", "").split("/");
        String[] splittedStringByColon = splittedStringBySlash[0].split(":");
        return Integer.parseInt(splittedStringByColon[1]);
    }

    public static List<String> extractOperations(String formula) {
        String[] splittedString = formula.replace(" ", "").split("/");
        return Arrays.asList(splittedString[0].split(":")[1].split(","));
    }

}
