package de.prob2.ui.symbolic;

import java.util.List;
import java.util.Optional;

import de.prob2.ui.project.machines.Machine;

public interface SymbolicFormulaHandler<T extends SymbolicItem<?>> {
	public List<T> getItems(Machine machine);
	
	public default Optional<T> addItem(final Machine machine, final T item) {
		final List<T> items = this.getItems(machine);
		final Optional<T> existingItem = items.stream().filter(item::settingsEqual).findAny();
		if(!existingItem.isPresent()) {
			items.add(item);
		}
		return existingItem;
	}
	
	public default Optional<T> replaceItem(final Machine machine, final T oldItem, final T newItem) {
		final List<T> items = this.getItems(machine);
		final Optional<T> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if(!existingItem.isPresent()) {
			items.set(items.indexOf(oldItem), newItem);
		}
		return existingItem;
	}
	
	public void handleItem(T item, boolean checkAll);
	public void handleMachine(Machine machine);
}
