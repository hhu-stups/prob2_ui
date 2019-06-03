package de.prob2.ui.animation.symbolic;


import de.prob2.ui.symbolic.SymbolicExecutionType;

import java.util.List;

public class OperationCoverageItem extends SymbolicAnimationFormulaItem implements ITestCaseGenerationItem {

    private List<String> operations;

    private String depth;

    public OperationCoverageItem(List<String> operations, String depth) {
        super("OPERATION:" + String.join(",", operations), "DEPTH: " + depth, SymbolicExecutionType.COVERED_OPERATIONS);
        this.operations = operations;
        this.depth = depth;
    }

    public List<String> getOperations() {
        return operations;
    }

    @Override
    public String getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "OPERATION: " + String.join(",", operations) + ", " + "DEPTH: " + depth;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }
}
