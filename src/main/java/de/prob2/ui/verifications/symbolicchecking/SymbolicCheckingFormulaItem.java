package de.prob2.ui.verifications.symbolicchecking;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;

public abstract class SymbolicCheckingFormulaItem extends AbstractCheckableItem implements ICliTask {
	protected SymbolicCheckingFormulaItem(String id) {
		super(id);
	}
}
