package de.prob2.ui.states;

public class SimpleStateTreeItem extends StateTreeItem<String> {
	public SimpleStateTreeItem(final String name, final String value, final String previousValue) {
		super(name, value, previousValue, name);
	}
	
	public SimpleStateTreeItem(final String name) {
		this(name, "", "");
	}
	
	public SimpleStateTreeItem() {
		this("");
	}
}
