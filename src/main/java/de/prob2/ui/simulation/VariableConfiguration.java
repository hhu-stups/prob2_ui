package de.prob2.ui.simulation;

import java.util.Map;

public class VariableConfiguration {

    private Map<String, String> values;

    private String probability;

    public VariableConfiguration(Map<String, String> values, String probability) {
        this.values = values;
        this.probability = probability;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public String getProbability() {
        return probability;
    }
}
