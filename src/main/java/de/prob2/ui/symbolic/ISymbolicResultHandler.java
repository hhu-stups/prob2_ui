package de.prob2.ui.symbolic;

import de.prob.animator.command.AbstractCommand;
import de.prob2.ui.verifications.AbstractResultHandler.ItemType;

public interface ISymbolicResultHandler {
	void handleFormulaResult(SymbolicFormulaItem item, AbstractCommand cmd);
	void handleFormulaResult(SymbolicFormulaItem item, Object result);
	void showAlreadyExists(ItemType formula);
}
