package de.prob2.ui.preferences;

public class RealPrefTreeItem extends PrefTreeItem {
	public RealPrefTreeItem(
		final String name,
		final String changed,
		final String value,
		final Class<?> valueType,
		final String defaultValue,
		final String description
	) {
		super(name, changed, value, valueType, defaultValue, description);
	}

	public RealPrefTreeItem(String name) {
		this(name, "", "", String.class, "", "");
	}

	public RealPrefTreeItem() {
		this("");
	}

	@Override
	public void updateValue(final Preferences prefs) {
		this.valueType.set(prefs.guessType(this.getName()));
		this.value.set(prefs.getPreferenceValue(this.getName()));
		this.changed.set(this.value.get().equals(this.defaultValue.get()) ? "" : "*");
	}
}
