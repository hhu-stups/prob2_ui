package de.prob2.ui.project.machines;

import java.util.List;

import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ISelectableTask;

public final class MachineCheckingStatus {
	private final Status status;
	private final int numberSuccess;
	private final int numberTotal;

	public MachineCheckingStatus(Status status, int numberSuccess, int numberTotal) {
		this.status = status;
		this.numberSuccess = numberSuccess;
		this.numberTotal = numberTotal;
	}

	public MachineCheckingStatus() {
		this.status = Status.NONE;
		this.numberSuccess = 0;
		this.numberTotal = 0;
	}

	private static Status combineCheckingStatus(List<? extends ISelectableTask> items) {
		boolean anyEnabled = false;
		boolean anyUnknown = false;
		for (ISelectableTask item : items) {
			if (!item.selected()) {
				continue;
			}
			anyEnabled = true;
			if (item.getStatus() == CheckingStatus.FAIL) {
				return Status.FAILED;
			} else if (item.getStatus() == CheckingStatus.NOT_CHECKED) {
				anyUnknown = true;
			}
		}
		return anyEnabled ? (anyUnknown ? Status.UNKNOWN : Status.SUCCESSFUL) : Status.NONE;
	}

	static MachineCheckingStatus combineMachineCheckingStatus(List<? extends ISelectableTask> items) {
		Status status = combineCheckingStatus(items);
		int numberSuccess = (int) items.stream()
			                          .filter(item -> item.getStatus() == CheckingStatus.SUCCESS && item.selected())
			                          .count();
		int numberTotal = (int) items.stream()
			                        .filter(ISelectableTask::selected)
			                        .count();
		return new MachineCheckingStatus(status, numberSuccess, numberTotal);
	}

	public Status getStatus() {
		return status;
	}

	public int getNumberSuccess() {
		return numberSuccess;
	}

	public int getNumberTotal() {
		return numberTotal;
	}

	public enum Status {
		UNKNOWN, SUCCESSFUL, FAILED, NONE
	}
}
