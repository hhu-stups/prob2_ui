package de.prob2.ui.commands;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

public class ExecuteRightClickCommand extends AbstractCommand {

	private static final String PROLOG_COMMAND_NAME = "react_to_item_right_click_option_for_state";
	private String stateId;
	private int row;
	private int column;
	private String option;
	private String transition = "TransitionId";
	private String newState = "NewStateId";
	private String transitionID;
	// private String newStateId;

	public ExecuteRightClickCommand(String stateId, int row, int column, String option) {
		this.stateId = stateId;
		this.row = row;
		this.column = column;
		this.option = option;
	}

	@Override
	public void writeCommand(IPrologTermOutput pto) {
		pto.openTerm(PROLOG_COMMAND_NAME);
		pto.printAtomOrNumber(stateId);
		pto.printNumber(row);
		pto.printNumber(column);
		pto.printAtom(option);
		pto.printVariable(transition);
		pto.printVariable(newState);
		pto.closeTerm();
	}

	@Override
	public void processResult(ISimplifiedROMap<String, PrologTerm> bindings) {
		transitionID = bindings.get(transition).getFunctor();
		// newStateId = bindings.get(newState).getFunctor();
	}

	public String getTransitionID() {
		return transitionID;
	}

}
