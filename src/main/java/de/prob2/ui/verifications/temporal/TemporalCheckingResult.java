package de.prob2.ui.verifications.temporal;

import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;

public class TemporalCheckingResult extends CheckingResult {
	private final List<ErrorItem> errorMarkers;
	
	public TemporalCheckingResult(CheckingStatus status, List<ErrorItem> errorMarkers, String messageBundleKey, Object... messageParams) {
		super(status, messageBundleKey, messageParams);
		this.errorMarkers = errorMarkers;
	}
	
	public List<ErrorItem> getErrorMarkers() {
		return errorMarkers;
	}

}
