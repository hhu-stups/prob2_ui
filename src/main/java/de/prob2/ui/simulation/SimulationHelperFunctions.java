package de.prob2.ui.simulation;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;

public class SimulationHelperFunctions {

    public static AbstractEvalResult evaluateForSimulation(State state, String formula) {
        // Note: Rodin parser does not have IF-THEN-ELSE nor REAL
        return state.eval(new ClassicalB(formula, FormulaExpand.TRUNCATE));
    }

}
