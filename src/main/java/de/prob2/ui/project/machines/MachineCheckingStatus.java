package de.prob2.ui.project.machines;

import java.util.List;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

public final class MachineCheckingStatus {

	private final CheckingStatus status;
	private final int numberSuccess;
	private final int numberTotal;
	public MachineCheckingStatus(CheckingStatus status, int numberSuccess, int numberTotal) {
		this.status = status;
		this.numberSuccess = numberSuccess;
		this.numberTotal = numberTotal;
	}

	public MachineCheckingStatus() {
		this.status = CheckingStatus.NONE;
		this.numberSuccess = 0;
		this.numberTotal = 0;
	}

	private static CheckingStatus combineCheckingStatus(final List<? extends IExecutableItem> items) {
		boolean anyEnabled = false;
		boolean anyUnknown = false;
		for (IExecutableItem item : items) {
			if (!item.selected()) {
				continue;
			}
			anyEnabled = true;
			if (item.getChecked() == Checked.FAIL) {
				return CheckingStatus.FAILED;
			} else if (item.getChecked() == Checked.NOT_CHECKED) {
				anyUnknown = true;
			}
		}
		return anyEnabled ? (anyUnknown ? CheckingStatus.UNKNOWN : CheckingStatus.SUCCESSFUL) : CheckingStatus.NONE;
	}

	static MachineCheckingStatus combineMachineCheckingStatus(final List<? extends IExecutableItem> items) {
		CheckingStatus status = combineCheckingStatus(items);
		int numberSuccess = (int) items.stream()
			                          .filter(item -> item.getChecked() == Checked.SUCCESS && item.selected())
			                          .count();
		int numberTotal = (int) items.stream()
			                        .filter(IExecutableItem::selected)
			                        .count();
		return new MachineCheckingStatus(status, numberSuccess, numberTotal);
	}

	public CheckingStatus getStatus() {
		return status;
	}

	public int getNumberSuccess() {
		return numberSuccess;
	}

	public int getNumberTotal() {
		return numberTotal;
	}

	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED, NONE
	}
}
