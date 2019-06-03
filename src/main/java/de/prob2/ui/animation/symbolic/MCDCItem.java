package de.prob2.ui.animation.symbolic;


import de.prob2.ui.symbolic.SymbolicExecutionType;

public class MCDCItem extends SymbolicAnimationFormulaItem implements ITestCaseGenerationItem {

    private String level;

    private String depth;

    public MCDCItem(String level, String depth) {
        super("MCDC:" + level, "DEPTH: " + depth ,SymbolicExecutionType.MCDC);
        this.level = level;
        this.depth = depth;
    }

    public String getLevel() {
        return level;
    }

    @Override
    public String getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "LEVEL: " + level + ", " + "DEPTH: " + depth;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }
}
