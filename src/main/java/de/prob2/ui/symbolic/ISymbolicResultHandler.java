package de.prob2.ui.symbolic;

import de.prob.animator.command.AbstractCommand;
import de.prob2.ui.verifications.AbstractResultHandler.ItemType;

public interface ISymbolicResultHandler {
	void handleFormulaResult(SymbolicItem item, AbstractCommand cmd);
	void handleFormulaResult(SymbolicItem item, Object result);
	void showAlreadyExists(ItemType formula);
}
