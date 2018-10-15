package de.prob2.ui.preferences;

public class RealPrefTreeItem extends PrefItem {
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
}
