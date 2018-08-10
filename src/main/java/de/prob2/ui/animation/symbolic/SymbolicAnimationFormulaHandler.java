package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
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
public class SymbolicAnimationFormulaHandler {

	private final CurrentTrace currentTrace;
	
	private final SymbolicAnimationChecker symbolicChecker;
	
	private final SymbolicAnimationResultHandler resultHandler;
	
	private final Injector injector;
	
	private final CurrentProject currentProject;
	
	
	@Inject
	public SymbolicAnimationFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
											final Injector injector, final SymbolicAnimationChecker symbolicChecker,
											final SymbolicAnimationResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.symbolicChecker = symbolicChecker;
		this.resultHandler = resultHandler;
	}
	
	public void addFormula(String name, SymbolicAnimationType type, boolean checking) {
		SymbolicAnimationFormulaItem formula = new SymbolicAnimationFormulaItem(name, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(SymbolicAnimationFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getSymbolicAnimationFormulas().contains(formula)) {
				currentMachine.addSymbolicAnimationFormula(formula);
				injector.getInstance(SymbolicAnimationView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}

	
	public void handleDeadlock(String code, boolean checkAll) {
		IEvalElement constraint = new EventB(code, FormulaExpand.EXPAND); 
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace(), constraint);
		symbolicChecker.executeCheckingItem(checker, code, SymbolicAnimationType.DEADLOCK, checkAll);
	}
	
	public void findDeadlock(boolean checkAll) {
		CBCDeadlockChecker checker = new CBCDeadlockChecker(currentTrace.getStateSpace());
		symbolicChecker.executeCheckingItem(checker, "FIND_DEADLOCK", SymbolicAnimationType.FIND_DEADLOCK, checkAll);
	}
	
	public void handleSequence(String sequence, boolean checkAll) {
		List<String> events = Arrays.asList(sequence.replaceAll(" ", "").split(";"));
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), events);
		symbolicChecker.executeCheckingItem(checker, sequence, SymbolicAnimationType.SEQUENCE, checkAll);
	}
	
	public void findRedundantInvariants(SymbolicAnimationFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		GetRedundantInvariantsCommand cmd = new GetRedundantInvariantsCommand();
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void findValidState(SymbolicAnimationFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode(), FormulaExpand.EXPAND), true);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}
	
	public void handleItem(SymbolicAnimationFormulaItem item, boolean checkAll) {
		if(!item.shouldExecute()) {
			return;
		}
		SymbolicAnimationType type = item.getType();
		switch(type) {
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
			case FIND_REDUNDANT_INVARIANTS:
				findRedundantInvariants(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
