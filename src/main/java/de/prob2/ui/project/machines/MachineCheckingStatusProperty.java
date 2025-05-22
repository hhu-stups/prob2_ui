package de.prob2.ui.project.machines;

import de.prob2.ui.verifications.ISelectableTask;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;

final class MachineCheckingStatusProperty extends ReadOnlyObjectPropertyBase<MachineCheckingStatus> {
	private final ObservableList<? extends ISelectableTask> items;

	// The listeners must be stored as instance fields and not local variables,
	// because they will be added as weak listeners and not automatically kept alive.
	// Without these references, the listeners would be garbage-collected soon after the property is created.
	@SuppressWarnings("FieldCanBeLocal")
	private final InvalidationListener changedListener;
	private final WeakInvalidationListener changedListenerWeak;
	@SuppressWarnings("FieldCanBeLocal")
	private final ListChangeListener<ISelectableTask> listChangeListener;

	MachineCheckingStatusProperty(ObservableList<? extends ISelectableTask> items) {
		this.items = items;

		this.changedListener = o -> Platform.runLater(this::fireValueChangedEvent);
		this.changedListenerWeak = new WeakInvalidationListener(this.changedListener);
		this.listChangeListener = change -> {
			while (change.next()) {
				change.getRemoved().forEach(item -> {
					item.selectedProperty().removeListener(this.changedListenerWeak);
					item.statusProperty().removeListener(this.changedListenerWeak);
				});
				change.getAddedSubList().forEach(item -> {
					item.selectedProperty().addListener(this.changedListenerWeak);
					item.statusProperty().addListener(this.changedListenerWeak);
				});
			}
			this.changedListenerWeak.invalidated(change.getList());
		};

		this.items.addListener(new WeakListChangeListener<>(this.listChangeListener));
		items.forEach(item -> {
			item.selectedProperty().addListener(this.changedListenerWeak);
			item.statusProperty().addListener(this.changedListenerWeak);
		});
	}

	@Override
	public Object getBean() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public MachineCheckingStatus get() {
		return MachineCheckingStatus.combineMachineCheckingStatus(this.items);
	}
}
