package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
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
import de.prob2.ui.verifications.AbstractResultHandler;

@Singleton
public class SymbolicCheckingFormulaHandler {
	
	private final CurrentTrace currentTrace;
	
	private final SymbolicFormulaChecker symbolicChecker;
	
	private final SymbolicCheckingResultHandler resultHandler;
	
	private final Injector injector;
	
	private final CurrentProject currentProject;
	
	
	@Inject
	public SymbolicCheckingFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
											final Injector injector, final SymbolicFormulaChecker symbolicChecker,
											final SymbolicCheckingResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.symbolicChecker = symbolicChecker;
		this.resultHandler = resultHandler;
	}
	
	public void addFormula(String name, String code, SymbolicCheckingType type, boolean checking) {
		SymbolicCheckingFormulaItem formula = new SymbolicCheckingFormulaItem(name, code, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(SymbolicCheckingFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getSymbolicCheckingFormulas().contains(formula)) {
				currentMachine.addSymbolicCheckingFormula(formula);
				injector.getInstance(SymbolicCheckingView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}
	
	public void handleInvariant(String code, boolean checkAll) {
		ArrayList<String> event = new ArrayList<>();
		event.add(code);
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		symbolicChecker.executeCheckingItem(checker, code, SymbolicCheckingType.INVARIANT, checkAll);
	}
	
	public void handleDeadlock(String code, boolean checkAll) {
		IEvalElement constraint = new EventB(code, FormulaExpand.EXPAND); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		symbolicChecker.executeCheckingItem(checker, code, SymbolicCheckingType.DEADLOCK, checkAll);
	}
	
	public void findDeadlock(boolean checkAll) {
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace());
		symbolicChecker.executeCheckingItem(checker, "FIND_DEADLOCK", SymbolicCheckingType.FIND_DEADLOCK, checkAll);
	}
	
	public void handleSequence(String sequence, boolean checkAll) {
		List<String> events = Arrays.asList(sequence.replaceAll(" ", "").split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		symbolicChecker.executeCheckingItem(checker, sequence, SymbolicCheckingType.SEQUENCE, checkAll);
	}
	
	public void findRedundantInvariants(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
		
	public void handleRefinement(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedRefinementCheckCommand cmd = new ConstraintBasedRefinementCheckCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleAssertions(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedAssertionCheckCommand cmd = new ConstraintBasedAssertionCheckCommand(stateSpace);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleSymbolic(SymbolicCheckingFormulaItem item, SymbolicModelcheckCommand.Algorithm algorithm, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		SymbolicModelcheckCommand cmd = new SymbolicModelcheckCommand(algorithm);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
		
	}
	
	public void findValidState(SymbolicCheckingFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode(), FormulaExpand.EXPAND), true);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleItem(SymbolicCheckingFormulaItem item, boolean checkAll) {
		if(!item.shouldExecute()) {
			return;
		}
		SymbolicCheckingType type = item.getType();
		switch(type) {
			case INVARIANT:
				handleInvariant(item.getCode(), checkAll);
				break;
			case DEADLOCK:
				handleDeadlock(item.getCode(), checkAll);
				break;
			case SEQUENCE:
				handleSequence(item.getCode(), checkAll);
				break;
			case FIND_VALID_STATE:
				findValidState(item, checkAll);
				break;
			case FIND_DEADLOCK:
				findDeadlock(checkAll);
				break;
			case CHECK_REFINEMENT:
				handleRefinement(item, checkAll);
				break;
			case CHECK_ASSERTIONS:
				handleAssertions(item, checkAll);
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
