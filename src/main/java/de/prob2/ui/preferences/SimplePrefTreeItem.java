package de.prob2.ui.preferences;

public class SimplePrefTreeItem extends PrefTreeItem {
	public SimplePrefTreeItem(String name, String value, String defaultValue, String description) {
		super(name, value, defaultValue, description);
	}

	public SimplePrefTreeItem(String name) {
		this(name, "", "", "");
	}

	public SimplePrefTreeItem() {
		this("");
	}

	@Override
	public void updateValue(final Preferences prefs) {
		this.value.set(prefs.getPreferenceValue(this.getName()));
	}
}
