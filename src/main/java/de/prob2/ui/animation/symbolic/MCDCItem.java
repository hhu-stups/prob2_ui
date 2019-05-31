package de.prob2.ui.animation.symbolic;


import de.prob2.ui.symbolic.SymbolicExecutionType;

public class MCDCItem extends SymbolicAnimationFormulaItem {

    private String level;

    private String depth;

    public MCDCItem(String level, String depth) {
        super("MCDC:" + level, SymbolicExecutionType.MCDC);
        this.level = level;
        this.depth = depth;
    }

    public String getLevel() {
        return level;
    }

    public String getDepth() {
        return depth;
    }
}
