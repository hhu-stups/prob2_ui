package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;

@Singleton
public class VOChecker {

    private final LTLFormulaChecker ltlChecker;

    @Inject
    public VOChecker(final LTLFormulaChecker ltlChecker) {
        this.ltlChecker = ltlChecker;
    }


    public void check(ValidationObligation validationObligation) {
        ValidationTask task = validationObligation.getTask();
        IExecutableItem item = validationObligation.getItem();
        switch (task) {
            case MODEL_CHECKING:
                break;
            case LTL_MODEL_CHECKING:
                ltlChecker.checkFormula((LTLFormulaItem) item);
                break;
            case SYMBOLIC_MODEL_CHECKING:
                break;
            case TRACE_REPLAY:
                break;
            default:
                throw new RuntimeException("Validation task is not valid: " + task);
        }
    }

}
