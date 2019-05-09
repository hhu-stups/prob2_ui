package de.prob2.ui.verifications.ltl;

import java.util.List;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

public class LTLCheckingResultItem extends CheckingResultItem {

	private List<LTLMarker> errorMarkers;
	
	public LTLCheckingResultItem(Checked checked, List<LTLMarker> errorMarkers, String headerBundleKey, String messageBundleKey,
			Object... messageParams) {
		super(checked, headerBundleKey, messageBundleKey, messageParams);
		this.errorMarkers = errorMarkers;
	}
	
	public List<LTLMarker> getErrorMarkers() {
		return errorMarkers;
	}

}
