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
	
	public static void updateMachineStatus(Machine machine, CheckingType type) {
		List<? extends IExecutableItem> items = getItems(machine, type);
		refreshMachineStatus(machine, type, combineCheckingStatus(items));
	}
	
	private static Machine.CheckingStatus combineCheckingStatus(final List<? extends IExecutableItem> items) {
		boolean anyEnabled = false;
		for(IExecutableItem item : items) {
			if(!item.selected()) {
				continue;
			}
			anyEnabled = true;
			if(item.getChecked() == Checked.FAIL) {
				return Machine.CheckingStatus.FAILED;
			} else if (item.getChecked() != Checked.SUCCESS) {
				return Machine.CheckingStatus.UNKNOWN;
			}
		}
		return anyEnabled ? Machine.CheckingStatus.SUCCESSFUL : Machine.CheckingStatus.NONE;
	}
	
	private static void refreshMachineStatus(Machine machine, CheckingType type, Machine.CheckingStatus status) {
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
	
	private static List<? extends IExecutableItem> getItems(Machine machine, CheckingType type) {
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
