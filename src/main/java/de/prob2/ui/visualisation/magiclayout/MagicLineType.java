package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.List;

public enum MagicLineType {
	
	CONTINUOUS(new Double[0]),
	DASHED(new Double[] { 12.0, 4.0, 5.0, 4.0 }),
	DOTTED(new Double[] { 2.0, 2.0, 2.0, 2.0 }),
	;

	private final Double[] dashArray;
	
	MagicLineType(final Double[] dashArray) {
		this.dashArray = dashArray;
	}
	
	public List<Double> getDashArrayList() {
		return Arrays.asList(dashArray);
	}
}
