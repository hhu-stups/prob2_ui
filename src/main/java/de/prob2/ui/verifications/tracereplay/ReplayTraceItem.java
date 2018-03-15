package de.prob2.ui.verifications.tracereplay;

import java.io.File;

public class ReplayTraceItem {
	private final File location;
	private final ReplayTrace trace;

	public ReplayTraceItem(ReplayTrace trace, File traceFile) {
		this.location = traceFile;
		this.trace = trace;
	}
	
	public File getLocation() {
		return location;
	}
	
	public ReplayTrace getTrace() {
		return trace;
	}
}
