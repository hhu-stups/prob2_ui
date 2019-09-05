package de.prob2.ui.verifications.modelchecking;

import de.prob2.ui.verifications.Checked;

public abstract class AbstractModelCheckingItem {
	protected Checked checked;

	public AbstractModelCheckingItem() {
		this.checked = Checked.NOT_CHECKED;
	}

	public Checked getChecked() {
		return checked;
	}

	public void setCheckedSuccessful() {
		this.checked = Checked.SUCCESS;
	}

	public void setCheckedFailed() {
		this.checked = Checked.FAIL;
	}

	public void setTimeout() {
		this.checked = Checked.TIMEOUT;
	}

}
