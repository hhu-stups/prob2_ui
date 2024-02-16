package de.prob2.ui.verifications.type;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.DynamicCommandFormulaItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.vomanager.ValidationTaskNotFound;

import static de.prob2.ui.verifications.type.ValidationTaskType.register;

public final class BuiltinValidationTaskTypes {

	public static final ValidationTaskType INVALID = register(new ValidationTaskType("INVALID", ValidationTaskNotFound.class));
	public static final ValidationTaskType TEMPORAL = register(new ValidationTaskType("TEMPORAL", TemporalFormulaItem.class));
	public static final ValidationTaskType SYMBOLIC = register(new ValidationTaskType("SYMBOLIC", SymbolicCheckingFormulaItem.class));
	public static final ValidationTaskType SIMULATION = register(new ValidationTaskType("SIMULATION", SimulationItem.class));
	public static final ValidationTaskType REPLAY_TRACE = register(new ValidationTaskType("REPLAY_TRACE", ReplayTrace.class));
	public static final ValidationTaskType MODEL_CHECKING = register(new ValidationTaskType("MODEL_CHECKING", ModelCheckingItem.class));
	public static final ValidationTaskType PROOF_OBLIGATION = register(new ValidationTaskType("PROOF_OBLIGATION", ProofObligationItem.class));
	public static final ValidationTaskType DYNAMIC_FORMULA = register(new ValidationTaskType("DYNAMIC_FORMULA", DynamicCommandFormulaItem.class));

	private BuiltinValidationTaskTypes() {
		throw new AssertionError();
	}
}
