package de.prob2.ui.preferences;

public class RealPrefTreeItem extends PrefTreeItem {
	public RealPrefTreeItem(
		final String name,
		final String changed,
		final String value,
		final ProBPreferenceType valueType,
		final String defaultValue,
		final String description
	) {
		super(name, changed, value, valueType, defaultValue, description);
	}
	
	@Override
	public void updateValue(final ProBPreferences prefs) {
		this.value.set(prefs.getPreferenceValue(this.getName()));
		this.changed.set(this.value.get().equals(this.defaultValue.get()) ? "" : "*");
	}
}
