package de.prob2.ui.vomanager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VOTemplateGenerator {

    public static List<ValidationTask> generate(RequirementType requirementType) {
        switch (requirementType) {
            case INVARIANT:
            case DEADLOCK_FREEDOM:
                return Arrays.asList(ValidationTask.MODEL_CHECKING, ValidationTask.LTL_MODEL_CHECKING, ValidationTask.SYMBOLIC_MODEL_CHECKING);
            case SAFETY:
                return Collections.singletonList(ValidationTask.LTL_MODEL_CHECKING);
            case LIVENESS:
            case FAIRNESS:
                return Collections.singletonList(ValidationTask.LTL_MODEL_CHECKING);
            case USE_CASE:
                return Collections.singletonList(ValidationTask.TRACE_REPLAY);
            case TIMING:
            case PROBABILISTIC:
            case TIMED_PROBABILISTIC:
                return Collections.singletonList(ValidationTask.SIMULATION);
            default:
                throw new RuntimeException("Requirement type is not valid: " + requirementType);
        }
    }

}
