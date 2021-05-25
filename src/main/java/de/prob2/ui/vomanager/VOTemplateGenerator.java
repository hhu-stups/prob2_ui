package de.prob2.ui.vomanager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VOTemplateGenerator {

    public static List<ValidationTask> generate(Requirement requirement) {
        if(requirement == null) {
            return Collections.emptyList();
        }
        RequirementType type = requirement.getType();
        switch (type) {
            case INVARIANT:
                return Arrays.asList(ValidationTask.MODEL_CHECKING, ValidationTask.LTL_MODEL_CHECKING, ValidationTask.SYMBOLIC_MODEL_CHECKING);
            case SAFETY:
                return Collections.singletonList(ValidationTask.LTL_MODEL_CHECKING);
            case LIVENESS:
                return Collections.singletonList(ValidationTask.LTL_MODEL_CHECKING);
            case USE_CASE:
                return Collections.singletonList(ValidationTask.TRACE_REPLAY);
            default:
                throw new RuntimeException("Requirement type is not valid: " + type);
        }
    }

}
