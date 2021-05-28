package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.Modelchecker;

@Singleton
public class VOChecker {

    private final Modelchecker modelchecker;

    private final LTLFormulaChecker ltlChecker;

    private final TraceChecker traceChecker;

    @Inject
    public VOChecker(final Modelchecker modelchecker, final LTLFormulaChecker ltlChecker, final TraceChecker traceChecker) {
        this.modelchecker = modelchecker;
        this.ltlChecker = ltlChecker;
        this.traceChecker = traceChecker;
    }


    public void check(ValidationObligation validationObligation) {
        ValidationTask task = validationObligation.getTask();
        IExecutableItem item = validationObligation.getItem();
        switch (task) {
            case MODEL_CHECKING:
                modelchecker.checkItem((ModelCheckingItem) item, false, false);
                break;
            case LTL_MODEL_CHECKING:
                ltlChecker.checkFormula((LTLFormulaItem) item);
                break;
            case SYMBOLIC_MODEL_CHECKING:
                break;
            case TRACE_REPLAY:
                traceChecker.check((ReplayTrace) item, true);
                break;
            default:
                throw new RuntimeException("Validation task is not valid: " + task);
        }
    }

}
