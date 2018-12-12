package de.prob2.ui.visualisation.magiclayout;

public enum MagicLineWidth {
	
	NARROW(0.5),
	DEFAULT(1.0),
	WIDE(2.0),
	EXTRA_WIDE(5.0)
	;

	private final Double width;
	
	MagicLineWidth(Double width) {
		this.width = width;
	}
	
	public Double getWidth() {
		return width;
	}
}
