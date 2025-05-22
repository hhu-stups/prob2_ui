package de.prob2.ui.verifications;

import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;

public class ErrorsResult extends CheckingResult {
	private final List<ErrorItem> errorMarkers;
	
	public ErrorsResult(CheckingStatus status, List<ErrorItem> errorMarkers, String messageBundleKey, Object... messageParams) {
		super(status, messageBundleKey, messageParams);
		this.errorMarkers = errorMarkers;
	}
	
	public List<ErrorItem> getErrorMarkers() {
		return errorMarkers;
	}

}
