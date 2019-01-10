package de.prob2.ui.visualisation.magiclayout;

public enum MagicShape {
	
	RECTANGLE("visualisation.magicLayout.shapes.rectangle"),
	CIRCLE("visualisation.magicLayout.shapes.circle"),
	ELLIPSE("visualisation.magicLayout.shapes.ellipse"),
	TRIANGLE("visualisation.magicLayout.shapes.triangle"),
	;

	private final String bundleKey;
	
	MagicShape(final String bundleKey) {
		this.bundleKey = bundleKey;
	}
	
	public String getBundleKey() {
		return bundleKey;
	}
}
