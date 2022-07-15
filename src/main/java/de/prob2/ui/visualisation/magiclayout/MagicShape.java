package de.prob2.ui.visualisation.magiclayout;

import de.prob2.ui.internal.Translatable;

public enum MagicShape implements Translatable {

	RECTANGLE("visualisation.magicLayout.shapes.rectangle"),
	CIRCLE("visualisation.magicLayout.shapes.circle"),
	ELLIPSE("visualisation.magicLayout.shapes.ellipse"),
	TRIANGLE("visualisation.magicLayout.shapes.triangle"),
	;

	private final String bundleKey;

	MagicShape(final String bundleKey) {
		this.bundleKey = bundleKey;
	}

	@Override
	public String getTranslationKey() {
		return bundleKey;
	}
}
