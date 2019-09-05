package de.prob2.ui.verifications.modelchecking;

import de.prob2.ui.verifications.Checked;

public abstract class AbstractModelCheckingItem {
	private Checked checked;

	public AbstractModelCheckingItem() {
		this.checked = Checked.NOT_CHECKED;
	}

	public Checked getChecked() {
		return checked;
	}

	public void setChecked(final Checked checked) {
		this.checked = checked;
	}
}
