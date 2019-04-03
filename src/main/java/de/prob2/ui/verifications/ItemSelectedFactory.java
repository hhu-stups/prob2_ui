package de.prob2.ui.verifications;

import com.google.inject.Injector;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ItemSelectedFactory implements Callback<TableColumn.CellDataFeatures<IExecutableItem, CheckBox>, ObservableValue<CheckBox>> {
	
	private CheckingType type;
	
	private final Injector injector;

	private final ISelectableCheckingView view;
	
	public ItemSelectedFactory(CheckingType type, final ISelectableCheckingView view) {
		this.type = type;
		this.injector = null;
		this.view = view;
	}
	
	public ItemSelectedFactory(CheckingType type, final Injector injector, final ISelectableCheckingView view) {
		this.injector = injector;
		this.type = type;
		this.view = view;
	}
	
	@Override
	public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<IExecutableItem, CheckBox> param) {
		IExecutableItem item = param.getValue();
		CheckBox checkBox = new CheckBox();
		checkBox.selectedProperty().setValue(item.selected());
		checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			item.setSelected(newValue);
			if(type != CheckingType.REPLAY) {
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, type);
			}
			view.updateSelectViews();
		});
		return new SimpleObjectProperty<>(checkBox);
	}
}
