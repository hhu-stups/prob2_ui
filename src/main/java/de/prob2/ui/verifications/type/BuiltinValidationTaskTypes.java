package de.prob2.ui.verifications.type;

import de.prob2.ui.animation.symbolic.CBCFindSequenceItem;
import de.prob2.ui.animation.symbolic.FindValidStateItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.MCDCItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.OperationCoverageItem;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.dynamic.VisualizationFormulaTask;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.WellDefinednessCheckingItem;
import de.prob2.ui.verifications.cbc.CBCDeadlockFreedomCheckingItem;
import de.prob2.ui.verifications.cbc.CBCDynamicAssertionCheckingItem;
import de.prob2.ui.verifications.cbc.CBCFindRedundantInvariantsItem;
import de.prob2.ui.verifications.cbc.CBCInvariantPreservationCheckingItem;
import de.prob2.ui.verifications.cbc.CBCRefinementCheckingItem;
import de.prob2.ui.verifications.cbc.CBCStaticAssertionCheckingItem;
import de.prob2.ui.verifications.modelchecking.ProBModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.TLCModelCheckingItem;
import de.prob2.ui.verifications.po.ProofObligationItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicModelCheckingItem;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.LTLFormulaItem;
import de.prob2.ui.vomanager.ValidationTaskNotFound;

import static de.prob2.ui.verifications.type.ValidationTaskType.register;

public final class BuiltinValidationTaskTypes {

	public static final ValidationTaskType<ValidationTaskNotFound> INVALID = register(new ValidationTaskType<>("INVALID", ValidationTaskNotFound.class));
	public static final ValidationTaskType<LTLFormulaItem> LTL = register(new ValidationTaskType<>("LTL", LTLFormulaItem.class));
	public static final ValidationTaskType<CTLFormulaItem> CTL = register(new ValidationTaskType<>("CTL", CTLFormulaItem.class));
	public static final ValidationTaskType<CBCInvariantPreservationCheckingItem> CBC_INVARIANT_PRESERVATION_CHECKING = register(new ValidationTaskType<>("CBC_INVARIANT_PRESERVATION_CHECKING", CBCInvariantPreservationCheckingItem.class));
	public static final ValidationTaskType<CBCDeadlockFreedomCheckingItem> CBC_DEADLOCK_FREEDOM_CHECKING = register(new ValidationTaskType<>("CBC_DEADLOCK_FREEDOM_CHECKING", CBCDeadlockFreedomCheckingItem.class));
	public static final ValidationTaskType<CBCRefinementCheckingItem> CBC_REFINEMENT_CHECKING = register(new ValidationTaskType<>("CBC_REFINEMENT_CHECKING", CBCRefinementCheckingItem.class));
	public static final ValidationTaskType<CBCStaticAssertionCheckingItem> CBC_STATIC_ASSERTION_CHECKING = register(new ValidationTaskType<>("CBC_STATIC_ASSERTION_CHECKING", CBCStaticAssertionCheckingItem.class));
	public static final ValidationTaskType<CBCDynamicAssertionCheckingItem> CBC_DYNAMIC_ASSERTION_CHECKING = register(new ValidationTaskType<>("CBC_DYNAMIC_ASSERTION_CHECKING", CBCDynamicAssertionCheckingItem.class));
	public static final ValidationTaskType<WellDefinednessCheckingItem> WELL_DEFINEDNESS_CHECKING = register(new ValidationTaskType<>("WELL_DEFINEDNESS_CHECKING", WellDefinednessCheckingItem.class));
	public static final ValidationTaskType<CBCFindRedundantInvariantsItem> CBC_FIND_REDUNDANT_INVARIANTS = register(new ValidationTaskType<>("CBC_FIND_REDUNDANT_INVARIANTS", CBCFindRedundantInvariantsItem.class));
	public static final ValidationTaskType<SymbolicModelCheckingItem> SYMBOLIC_MODEL_CHECKING = register(new ValidationTaskType<>("SYMBOLIC_MODEL_CHECKING", SymbolicModelCheckingItem.class));
	public static final ValidationTaskType<CBCFindSequenceItem> CBC_FIND_SEQUENCE = register(new ValidationTaskType<>("CBC_FIND_SEQUENCE", CBCFindSequenceItem.class));
	public static final ValidationTaskType<FindValidStateItem> FIND_VALID_STATE = register(new ValidationTaskType<>("FIND_VALID_STATE", FindValidStateItem.class));
	public static final ValidationTaskType<MCDCItem> TEST_CASE_GENERATION_MCDC = register(new ValidationTaskType<>("TEST_CASE_GENERATION_MCDC", MCDCItem.class));
	public static final ValidationTaskType<OperationCoverageItem> TEST_CASE_GENERATION_OPERATION_COVERAGE = register(new ValidationTaskType<>("TEST_CASE_GENERATION_OPERATION_COVERAGE", OperationCoverageItem.class));
	public static final ValidationTaskType<SimulationItem> SIMULATION = register(new ValidationTaskType<>("SIMULATION", SimulationItem.class));
	public static final ValidationTaskType<ReplayTrace> REPLAY_TRACE = register(new ValidationTaskType<>("REPLAY_TRACE", ReplayTrace.class));
	public static final ValidationTaskType<ProBModelCheckingItem> MODEL_CHECKING = register(new ValidationTaskType<>("MODEL_CHECKING", ProBModelCheckingItem.class));
	public static final ValidationTaskType<TLCModelCheckingItem> TLC_MODEL_CHECKING = register(new ValidationTaskType<>("TLC_MODEL_CHECKING", TLCModelCheckingItem.class));
	public static final ValidationTaskType<ProofObligationItem> PROOF_OBLIGATION = register(new ValidationTaskType<>("PROOF_OBLIGATION", ProofObligationItem.class));
	public static final ValidationTaskType<VisualizationFormulaTask> VISUALIZATION_FORMULA = register(new ValidationTaskType<>("VISUALIZATION_FORMULA", VisualizationFormulaTask.class));

	private BuiltinValidationTaskTypes() {
		throw new AssertionError();
	}

	public static void init() {
		// NO-OP for classloading
	}
}
