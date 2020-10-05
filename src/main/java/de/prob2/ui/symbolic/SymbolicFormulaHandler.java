package de.prob2.ui.symbolic;

import java.util.List;

import de.prob2.ui.project.machines.Machine;

public interface SymbolicFormulaHandler<T extends SymbolicItem> {
	public List<T> getItems(Machine machine);
	
	public default void addItem(final Machine machine, final T item) {
		final List<T> items = this.getItems(machine);
		if(items.stream().noneMatch(item::settingsEqual)) {
			items.add(item);
		}
	}
	
	public default boolean replaceItem(final Machine machine, final T oldItem, final T newItem) {
		final List<T> items = this.getItems(machine);
		if(items.stream().noneMatch(newItem::settingsEqual)) {
			items.set(items.indexOf(oldItem), newItem);
			return true;
		}
		return false;
	}
	
	public void handleItem(T item, boolean checkAll);
	public void handleMachine(Machine machine);
}
