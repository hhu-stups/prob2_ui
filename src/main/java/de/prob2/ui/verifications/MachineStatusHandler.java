package de.prob2.ui.verifications;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.statusbar.StatusBar;

@Singleton
public final class MachineStatusHandler {
	private final Injector injector;
	
	@Inject
	private MachineStatusHandler(final Injector injector) {
		this.injector = injector;
	}
	
	public void updateMachineStatus(Machine machine, CheckingType type) {
		List<? extends IExecutableItem> items = getItems(machine, type);
		for(IExecutableItem item : items) {
			if(!item.shouldExecute()) {
				continue;
			}
			if(item.getChecked() == Checked.FAIL || item.getChecked() == Checked.EXCEPTION) {
				refreshMachineStatus(machine, type, Machine.CheckingStatus.FAILED);
				updateStatusBar(type, StatusBar.CheckingStatus.ERROR);
				return;
			}
		}
		refreshMachineStatus(machine, type, Machine.CheckingStatus.SUCCESSFUL);
		updateStatusBar(type, StatusBar.CheckingStatus.SUCCESSFUL);
	}
	
	private void updateStatusBar(CheckingType type, StatusBar.CheckingStatus status) {
		switch(type) {
			case LTL:
				injector.getInstance(StatusBar.class).setLtlStatus(status);
				break;
			case SYMBOLIC:
				injector.getInstance(StatusBar.class).setSymbolicStatus(status);
				break;
			case MODELCHECKING:
				injector.getInstance(StatusBar.class).setModelcheckingStatus(status);
				break;
			default:
				break;
		}
	}
	
	private void refreshMachineStatus(Machine machine, CheckingType type, Machine.CheckingStatus status) {
		switch(type) {
			case LTL:
				machine.setLtlStatus(status);
				break;
			case SYMBOLIC:
				machine.setSymbolicCheckingStatus(status);
				break;
			case MODELCHECKING:
				machine.setModelcheckingStatus(status);
				break;
			default:
				break;
		}
		injector.getInstance(MachineTableView.class).refresh();
	}
	
	private List<? extends IExecutableItem> getItems(Machine machine, CheckingType type) {
		switch(type) {
			case LTL:
				return machine.getLTLFormulas();
			case SYMBOLIC:
				return machine.getSymbolicCheckingFormulas();
			case MODELCHECKING:
				return machine.getModelcheckingItems();
			default:
				break;
		}
		return null;
		
	}
}
