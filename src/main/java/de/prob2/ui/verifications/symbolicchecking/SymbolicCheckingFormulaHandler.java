package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.CheckWellDefinednessCommand;
import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.CBCDeadlockChecker;
import de.prob.check.CBCInvariantChecker;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;

@Singleton
public class SymbolicCheckingFormulaHandler implements SymbolicFormulaHandler<SymbolicCheckingFormulaItem> {
	
	private final CurrentTrace currentTrace;
	
	private final SymbolicFormulaChecker symbolicChecker;
	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace,
											final SymbolicFormulaChecker symbolicChecker,
											final SymbolicCheckingResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.symbolicChecker = symbolicChecker;
	}
	
	@Override
	public List<SymbolicCheckingFormulaItem> getItems(final Machine machine) {
		return machine.getSymbolicCheckingFormulas();
	}
	
	public void handleInvariant(SymbolicCheckingFormulaItem item, boolean checkAll) {
		final ArrayList<String> eventNames;
		if (item.getCode().isEmpty()) {
			// Check all operations/events
			eventNames = null;
		} else {
			// Check only one specific operation/event
			eventNames = new ArrayList<>();
			eventNames.add(item.getCode());
		}
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), eventNames);
		symbolicChecker.checkItem(checker, item, checkAll);
	}
		
	public void handleRefinement(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleStaticAssertions(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.STATIC, stateSpace);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}

	public void handleDynamicAssertions(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(ConstraintBasedAssertionCheckCommand.CheckingType.DYNAMIC, stateSpace);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}

	public void handleWellDefinedness(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		CheckWellDefinednessCommand cmd = new CheckWellDefinednessCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleSymbolic(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		final SymbolicModelcheckCommand.Algorithm algorithm = SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode());
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
		
	}
	
	public void handleDeadlock(SymbolicCheckingFormulaItem item, boolean checkAll) {
		IEvalElement constraint = new EventB(item.getCode(), FormulaExpand.EXPAND); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		symbolicChecker.checkItem(checker, item, checkAll);
	}
	
	public void findRedundantInvariants(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		SymbolicExecutionType type = item.getType();
		switch(type) {
			case INVARIANT:
				handleInvariant(item, checkAll);
				break;
			case CHECK_REFINEMENT:
				handleRefinement(item, checkAll);
				break;
			case CHECK_STATIC_ASSERTIONS:
				handleStaticAssertions(item, checkAll);
				break;
			case CHECK_DYNAMIC_ASSERTIONS:
				handleDynamicAssertions(item, checkAll);
				break;
			case CHECK_WELL_DEFINEDNESS:
				handleWellDefinedness(item, checkAll);
				break;
			case DEADLOCK:
				handleDeadlock(item, checkAll);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item, checkAll);
				break;	
			case SYMBOLIC_MODEL_CHECK:
				handleSymbolic(item, checkAll);
				break;
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + type);
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(item -> handleItem(item, true));
	}
		
}
