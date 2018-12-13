package de.prob2.ui.visualisation.magiclayout;

public enum MagicLayout {
	
	LAYERED("visualisation.magicLayout.layout.layered"),
	RANDOM("visualisation.magicLayout.layout.random"),
	;

	private final String bundleKey;
	
	MagicLayout(final String bundleKey) {
		this.bundleKey = bundleKey;
	}
	
	public String getBundleKey() {
		return bundleKey;
	}
}
