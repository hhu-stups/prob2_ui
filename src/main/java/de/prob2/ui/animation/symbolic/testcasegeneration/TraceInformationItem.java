package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

public class TraceInformationItem {

    private int depth;

    private String transitions;

    private boolean complete;

    private boolean lastTransitionFeasible;

    public TraceInformationItem(int depth, List<String> transitions, boolean complete, boolean lastTransitionFeasible) {
        this.depth = depth;
        this.transitions = String.join(",\n", transitions);
        this.complete = complete;
        this.lastTransitionFeasible = lastTransitionFeasible;
    }

    public int getDepth() {
        return depth;
    }

    public String getTransitions() {
        return transitions;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isLastTransitionFeasible() {
        return lastTransitionFeasible;
    }
}
