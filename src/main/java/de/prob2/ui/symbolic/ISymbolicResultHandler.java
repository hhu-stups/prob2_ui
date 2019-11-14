package de.prob2.ui.symbolic;

import de.prob.animator.command.AbstractCommand;

public interface ISymbolicResultHandler {
	void handleFormulaResult(SymbolicItem item, AbstractCommand cmd);
	void handleFormulaResult(SymbolicItem item, Object result);
}
