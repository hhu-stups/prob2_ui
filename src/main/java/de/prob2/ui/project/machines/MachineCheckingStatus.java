package de.prob2.ui.project.machines;

public class MachineCheckingStatus {

	public enum CheckingStatus {
		UNKNOWN, SUCCESSFUL, FAILED, NONE
	}

	private final CheckingStatus status;
	private final int numberSuccess;
	private final int numberTotal;

	public MachineCheckingStatus(CheckingStatus status, int numberSuccess, int numberTotal) {
		this.status = status;
		this.numberSuccess = numberSuccess;
		this.numberTotal = numberTotal;
	}

	public MachineCheckingStatus(CheckingStatus status) {
		this.status = status;
		this.numberSuccess = 0;
		this.numberTotal = 0;
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
}
