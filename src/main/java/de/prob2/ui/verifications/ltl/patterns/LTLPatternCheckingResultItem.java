package de.prob2.ui.verifications.ltl.patterns;

import java.util.List;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLMarker;

public class LTLPatternCheckingResultItem extends CheckingResultItem {

	private List<LTLMarker> errorMarkers;
	
	public LTLPatternCheckingResultItem(Checked checked, List<LTLMarker> errorMarkers, String headerBundleKey, String messageBundleKey,
			Object... messageParams) {
		super(checked, headerBundleKey, messageBundleKey, messageParams);
		this.errorMarkers = errorMarkers;
	}
	
	public List<LTLMarker> getErrorMarkers() {
		return errorMarkers;
	}

}
