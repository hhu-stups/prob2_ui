package de.prob2.ui.animation.symbolic;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;

@Singleton
public class SymbolicAnimationItemHandler implements SymbolicFormulaHandler<SymbolicAnimationItem> {

	private final CurrentTrace currentTrace;

	private final SymbolicAnimationChecker symbolicChecker;

	private final CurrentProject currentProject;

	@Inject
	private SymbolicAnimationItemHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
										   final SymbolicAnimationChecker symbolicChecker,
										   final SymbolicAnimationResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.symbolicChecker = symbolicChecker;
	}

	@Override
	public void addFormula(SymbolicAnimationItem formula) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(currentMachine.getSymbolicAnimationFormulas().stream().noneMatch(formula::settingsEqual)) {
				currentMachine.getSymbolicAnimationFormulas().add(formula);
			}
		}
	}

	public void handleSequence(SymbolicAnimationItem item, boolean checkAll) {
		List<String> events = Arrays.asList(item.getCode().replace(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(currentTrace.getStateSpace(), events, new EventB("true", FormulaExpand.EXPAND));
		symbolicChecker.checkItem(item, cmd, currentTrace.getStateSpace(), checkAll);
	}

	public void findValidState(SymbolicAnimationItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode(), FormulaExpand.EXPAND), true);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}

	public void handleItem(SymbolicAnimationItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		SymbolicExecutionType type = item.getType();
		switch(type) {
			case SEQUENCE:
				handleSequence(item, checkAll);
				break;
			case FIND_VALID_STATE:
				findValidState(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
