package de.prob2.ui.symbolic;

import de.prob2.ui.project.machines.Machine;

public interface SymbolicFormulaHandler<T extends SymbolicFormulaItem> {

	public void handleItem(T item, boolean checkAll);
	public void handleMachine(Machine machine);
}
