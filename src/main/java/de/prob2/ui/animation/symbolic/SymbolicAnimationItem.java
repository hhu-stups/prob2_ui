package de.prob2.ui.animation.symbolic;

import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.ICliTask;

public abstract class SymbolicAnimationItem extends AbstractCheckableItem implements ICliTask {
	protected SymbolicAnimationItem(String id) {
		super(id);
	}
}
