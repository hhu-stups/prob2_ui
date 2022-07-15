package de.prob2.ui.visualisation.magiclayout;

import de.prob2.ui.internal.Translatable;

public enum MagicLayout implements Translatable {
	
	LAYERED("visualisation.magicLayout.layout.layered"),
	RANDOM("visualisation.magicLayout.layout.random"),
	;

	private final String bundleKey;
	
	MagicLayout(final String bundleKey) {
		this.bundleKey = bundleKey;
	}

	@Override
	public String getTranslationKey() {
		return bundleKey;
	}
}
