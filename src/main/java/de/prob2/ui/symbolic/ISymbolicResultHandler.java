package de.prob2.ui.symbolic;

import de.prob.animator.command.AbstractCommand;

public interface ISymbolicResultHandler<T extends SymbolicItem<?>> {
	void handleFormulaResult(T item, AbstractCommand cmd);
	void handleFormulaResult(T item, Object result);
}
