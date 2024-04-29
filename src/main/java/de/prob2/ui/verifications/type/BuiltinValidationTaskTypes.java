package de.prob2.ui.verifications.type;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.dotty.DotFormulaTask;
import de.prob2.ui.dynamic.plantuml.PlantUmlFormulaTask;
import de.prob2.ui.dynamic.table.TableFormulaTask;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.vomanager.ValidationTaskNotFound;

import static de.prob2.ui.verifications.type.ValidationTaskType.register;

public final class BuiltinValidationTaskTypes {

	public static final ValidationTaskType<ValidationTaskNotFound> INVALID = register(new ValidationTaskType<>("INVALID", ValidationTaskNotFound.class));
	public static final ValidationTaskType<TemporalFormulaItem> TEMPORAL = register(new ValidationTaskType<>("TEMPORAL", TemporalFormulaItem.class));
	public static final ValidationTaskType<SymbolicCheckingFormulaItem> SYMBOLIC = register(new ValidationTaskType<>("SYMBOLIC", SymbolicCheckingFormulaItem.class));
	public static final ValidationTaskType<SimulationItem> SIMULATION = register(new ValidationTaskType<>("SIMULATION", SimulationItem.class));
	public static final ValidationTaskType<ReplayTrace> REPLAY_TRACE = register(new ValidationTaskType<>("REPLAY_TRACE", ReplayTrace.class));
	public static final ValidationTaskType<ModelCheckingItem> MODEL_CHECKING = register(new ValidationTaskType<>("MODEL_CHECKING", ModelCheckingItem.class));
	public static final ValidationTaskType<ProofObligationItem> PROOF_OBLIGATION = register(new ValidationTaskType<>("PROOF_OBLIGATION", ProofObligationItem.class));
	public static final ValidationTaskType<DotFormulaTask> DOT_FORMULA = register(new ValidationTaskType<>("DOT_FORMULA", DotFormulaTask.class));
	public static final ValidationTaskType<PlantUmlFormulaTask> PLANTUML_FORMULA = register(new ValidationTaskType<>("PLANTUML_FORMULA", PlantUmlFormulaTask.class));
	public static final ValidationTaskType<TableFormulaTask> TABLE_FORMULA = register(new ValidationTaskType<>("TABLE_FORMULA", TableFormulaTask.class));

	private BuiltinValidationTaskTypes() {
		throw new AssertionError();
	}

	public static void init() {
		// NO-OP for classloading
	}
}
