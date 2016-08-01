package de.prob2.ui.preferences;

public class RealPrefTreeItem extends PrefTreeItem {
	public RealPrefTreeItem(
		final String name,
		final String changed,
		final String value,
		final PreferenceType valueType,
		final String defaultValue,
		final String description
	) {
		super(name, changed, value, valueType, defaultValue, description);
	}
	
	@Override
	public void updateValue(final Preferences prefs) {
		this.value.set(prefs.getPreferenceValue(this.getName()));
		this.changed.set(this.value.get().equals(this.defaultValue.get()) ? "" : "*");
	}
}
