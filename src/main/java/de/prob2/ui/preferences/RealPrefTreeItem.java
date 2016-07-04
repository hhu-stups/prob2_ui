package de.prob2.ui.preferences;

public class RealPrefTreeItem extends PrefTreeItem {
	public RealPrefTreeItem(String name, String value, String defaultValue, String description) {
		super(name, value, defaultValue, description);
	}

	public RealPrefTreeItem(String name) {
		this(name, "", "", "");
	}

	public RealPrefTreeItem() {
		this("");
	}

	@Override
	public void updateValue(final Preferences prefs) {
		this.value.set(prefs.getPreferenceValue(this.getName()));
	}
}
