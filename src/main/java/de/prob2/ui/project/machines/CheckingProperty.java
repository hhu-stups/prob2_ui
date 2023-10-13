package de.prob2.ui.project.machines;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

public class CheckingProperty<T> {
	private final ObjectProperty<MachineCheckingStatus> status;

	private final ListProperty<T> item;

	public CheckingProperty(ObjectProperty<MachineCheckingStatus> status, ListProperty<T> item) {
		this.status = status;
		this.item = item;
	}

	public MachineCheckingStatus getStatus() {
		return status.get();
	}

	public ObjectProperty<MachineCheckingStatus> statusProperty() {
		return status;
	}

	public void setStatus(MachineCheckingStatus status) {
		this.status.set(status);
	}

	public ObservableList<T> getItem() {
		return item.get();
	}

	public ListProperty<T> itemProperty() {
		return item;
	}

	public void setItem(ObservableList<T> item) {
		this.item.set(item);
	}
}
