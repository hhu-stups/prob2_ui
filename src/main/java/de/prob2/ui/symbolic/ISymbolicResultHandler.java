package de.prob2.ui.symbolic;

import de.prob.animator.command.AbstractCommand;

public interface ISymbolicResultHandler {
	void handleFormulaResult(SymbolicFormulaItem item, AbstractCommand cmd);
	void handleFormulaResult(SymbolicFormulaItem item, Object result);
}
