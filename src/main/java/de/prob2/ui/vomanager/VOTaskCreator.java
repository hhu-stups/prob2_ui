package de.prob2.ui.vomanager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaStage;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.ModelcheckingStage;

@Singleton
public class VOTaskCreator {

    private final Injector injector;

    @Inject
    public VOTaskCreator(final Injector injector) {
        this.injector = injector;
    }

    public ValidationObligation openTaskWindow(Requirement requirement, ValidationTask task) {
        if(task == null) {
            // TODO: Show error message
            return null;
        }
        switch (task) {
            case MODEL_CHECKING: {
                ModelcheckingStage stageController = injector.getInstance(ModelcheckingStage.class);
                stageController.linkRequirement(requirement);
                stageController.showAndWait();
                ModelCheckingItem item = stageController.getLastItem();
                if(item == null) {
                    return null;
                }
                ValidationObligation validationObligation = new ValidationObligation(task, VOManager.extractConfiguration(item), item);
                validationObligation.setExecutable(item);
                return validationObligation;
            }
            case LTL_MODEL_CHECKING: {
                LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
                formulaStage.linkRequirement(requirement);
                formulaStage.setHandleItem(new LTLHandleItem<>(LTLHandleItem.HandleType.ADD, null));
                formulaStage.showAndWait();
                LTLFormulaItem item = formulaStage.getLastItem();
                if(item == null) {
                    return null;
                }
                ValidationObligation validationObligation = new ValidationObligation(task, VOManager.extractConfiguration(item), item);
                validationObligation.setExecutable(item);
                return validationObligation;
            }
            case SYMBOLIC_MODEL_CHECKING:
                // TODO: Implement
                break;
            case TRACE_REPLAY:
                // TODO: Implement
                break;
            default:
                throw new RuntimeException("Validation task is not valid");
        }
        return null;
    }

}
