package de.prob2.ui.simulation;

import java.util.List;

public class VariableChoice {

    private List<VariableConfiguration> choice;

    public VariableChoice(List<VariableConfiguration> choice) {
        this.choice = choice;
    }

    public List<VariableConfiguration> getChoice() {
        return choice;
    }
}
