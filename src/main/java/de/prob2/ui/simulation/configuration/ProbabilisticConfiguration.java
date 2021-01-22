package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class ProbabilisticConfiguration {

    private String opName;

    private Object probability;

    public ProbabilisticConfiguration(String opName, Object probability) {
        this.opName = opName;
        this.probability = probability;
    }

    public String getOpName() {
        return opName;
    }

    public Object getProbability() {
        return probability;
    }

}
