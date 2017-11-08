package de.prob2.ui.verifications;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.statusbar.StatusBar.CheckingStatus;

@Singleton
public class MachineStatusHandler {
	
	private final Injector injector;
	
	@Inject
	private MachineStatusHandler(final Injector injector) {
		this.injector = injector;
	}
	
	public void updateMachineStatus(Machine machine, Type type) {
		List<? extends IExecutableItem> items = getItems(machine, type);
		for(IExecutableItem item : items) {
			if(!item.shouldExecute()) {
				continue;
			}
			if(item.getChecked() == Checked.FAIL || item.getChecked() == Checked.EXCEPTION) {
				refreshMachineStatusFailed(machine, type);
				updateStatusBar(type, StatusBar.CheckingStatus.ERROR);
				return;
			}
		}
		refreshMachineStatusSuccess(machine, type);
		updateStatusBar(type, StatusBar.CheckingStatus.SUCCESSFUL);
	}
	
	private void updateStatusBar(Type type, CheckingStatus status) {
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
	
	private void refreshMachineStatusFailed(Machine machine, Type type) {
		switch(type) {
			case LTL:
				machine.setLTLCheckedFailed();
				break;
			case SYMBOLIC:
				machine.setSymbolicCheckedFailed();
				break;
			case MODELCHECKING:
				machine.setModelcheckingCheckedFailed();
				break;
			default:
				break;
		}
		injector.getInstance(MachineTableView.class).refresh();
	}
	
	private void refreshMachineStatusSuccess(Machine machine, Type type) {
		switch(type) {
			case LTL:
				machine.setLTLCheckedSuccessful();
				break;
			case SYMBOLIC:
				machine.setSymbolicCheckedSuccessful();
				break;
			case MODELCHECKING:
				machine.setModelcheckingCheckedSuccessful();
				break;
			default:
				break;
		}
		injector.getInstance(MachineTableView.class).refresh();
	}
	
	private List<? extends IExecutableItem> getItems(Machine machine, Type type) {
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
