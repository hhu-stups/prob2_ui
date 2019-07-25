package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class TestCaseGenerationFormulaExtractor {

    public String extractRawFormula(String formula) {
        return formula.replaceAll(" ", "").split("/")[0];
    }

    public String extractMCDCFormula(String level, String depth) {
    	try {
    		Integer.parseInt(level);
    		Integer.parseInt(depth);
    	} catch(NumberFormatException e) {
    		return "";
    	}
        return "MCDC:" + level + "/" + "DEPTH:" + depth;
    }

    public String extractOperationCoverageFormula(List<String> operations, String depth) {
    	try {
    		Integer.parseInt(depth);
    	} catch(NumberFormatException e) {
    		return "";
    	}
        return "OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + depth;
    }

    public String extractDepth(String formula) {
        String[] splittedStringBySlash = formula.replaceAll(" ", "").split("/");
        if(splittedStringBySlash.length < 2) {
        	return "";
        }
        String[] splittedStringByColon = splittedStringBySlash[1].split(":");
        if(splittedStringByColon.length < 2) {
        	return "";
        }
        return splittedStringByColon[1];
    }

    public String extractLevel(String formula) {
        String[] splittedStringBySlash = formula.replaceAll(" ", "").split("/");
        String[] splittedStringByColon = splittedStringBySlash[0].split(":");
        if(splittedStringByColon.length < 2) {
        	return "";
        }
        return splittedStringByColon[1];
    }

    public List<String> extractOperations(String formula) {
        String[] splittedString = formula.replaceAll(" ", "").split("/");
        return Arrays.asList(splittedString[0].split(":")[1].split(","));
    }
}
