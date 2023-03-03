package de.prob2.ui.verifications.temporal;

import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;

public class TemporalCheckingResultItem extends CheckingResultItem {

	private List<ErrorItem> errorMarkers;
	
	public TemporalCheckingResultItem(Checked checked, List<ErrorItem> errorMarkers, String messageBundleKey,
									  Object... messageParams) {
		super(checked, messageBundleKey, messageParams);
		this.errorMarkers = errorMarkers;
	}
	
	public List<ErrorItem> getErrorMarkers() {
		return errorMarkers;
	}

}
