package de.prob2.ui.verifications;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.project.machines.Machine;

@Singleton
public final class MachineStatusHandler {
	@Inject
	private MachineStatusHandler() {}
	
	public void updateMachineStatus(Machine machine, CheckingType type) {
		List<? extends IExecutableItem> items = getItems(machine, type);
		for(IExecutableItem item : items) {
			if(!item.selected()) {
				continue;
			}
			if(item.getChecked() == Checked.FAIL) {
				refreshMachineStatus(machine, type, Machine.CheckingStatus.FAILED);
				return;
			} else if (item.getChecked() != Checked.SUCCESS) {
				refreshMachineStatus(machine, type, Machine.CheckingStatus.UNKNOWN);
				return;
			}
		}
		refreshMachineStatus(machine, type, Machine.CheckingStatus.SUCCESSFUL);
	}
	
	private void refreshMachineStatus(Machine machine, CheckingType type, Machine.CheckingStatus status) {
		switch(type) {
			case LTL:
				machine.setLtlStatus(status);
				break;
			case SYMBOLIC_CHECKING:
				machine.setSymbolicCheckingStatus(status);
				break;
			case MODELCHECKING:
				machine.setModelcheckingStatus(status);
				break;
			default:
				break;
		}
	}
	
	private List<? extends IExecutableItem> getItems(Machine machine, CheckingType type) {
		switch(type) {
			case LTL:
				return machine.getLTLFormulas();
			case SYMBOLIC_CHECKING:
				return machine.getSymbolicCheckingFormulas();
			case MODELCHECKING:
				return machine.getModelcheckingItems();
			default:
				break;
		}
		return Collections.emptyList();
		
	}
}
