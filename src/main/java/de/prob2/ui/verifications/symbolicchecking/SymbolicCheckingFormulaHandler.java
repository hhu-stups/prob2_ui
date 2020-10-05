package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;

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
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;

@Singleton
public class SymbolicCheckingFormulaHandler implements SymbolicFormulaHandler<SymbolicCheckingFormulaItem> {
	
	private final CurrentTrace currentTrace;
	
	private final SymbolicFormulaChecker symbolicChecker;
	
	private final CurrentProject currentProject;
	
	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
											final SymbolicFormulaChecker symbolicChecker,
											final SymbolicCheckingResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.symbolicChecker = symbolicChecker;
	}
	
	public void addFormula(SymbolicCheckingFormulaItem formula) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(currentMachine.getSymbolicCheckingFormulas().stream().noneMatch(formula::settingsEqual)) {
				currentMachine.getSymbolicCheckingFormulas().add(formula);
			}
		}
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
	
	public void handleSymbolic(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand.Algorithm algorithm, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
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
			default:
				SymbolicModelcheckCommand.Algorithm algorithm = type.getAlgorithm();
				if(algorithm != null) {
					handleSymbolic(item, algorithm, checkAll);
					break;
				}
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicCheckingFormulas().forEach(item -> handleItem(item, true));
	}
		
}
