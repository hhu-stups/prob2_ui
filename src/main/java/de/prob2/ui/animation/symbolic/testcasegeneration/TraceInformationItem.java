package de.prob2.ui.animation.symbolic.testcasegeneration;

import de.prob.statespace.Trace;

import java.util.List;

public class TraceInformationItem {

    private int depth;

    private String transitions;

    private boolean complete;

    private boolean lastTransitionFeasible;

    private transient Trace trace;

    public TraceInformationItem(int depth, List<String> transitions, boolean complete, boolean lastTransitionFeasible, Trace trace) {
        this.depth = depth;
        this.transitions = String.join(",\n", transitions);
        this.complete = complete;
        this.lastTransitionFeasible = lastTransitionFeasible;
        this.trace = trace;
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

    public Trace getTrace() {
        return trace;
    }

}
