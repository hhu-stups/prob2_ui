package de.prob2.ui.verifications;

import java.util.Collections;
import java.util.List;

import de.prob.statespace.Trace;

public final class TraceResult extends CheckingResult {
	private final List<Trace> traces;
	
	public TraceResult(CheckingStatus status, List<Trace> traces, String messageBundleKey, Object... messageParams) {
		super(status, messageBundleKey, messageParams);
		
		this.traces = traces;
	}
	
	public TraceResult(CheckingStatus status, List<Trace> traces) {
		this(status, traces, status.getTranslationKey());
	}
	
	public TraceResult(CheckingStatus status, Trace trace, String messageBundleKey, Object... messageParams) {
		this(status, Collections.singletonList(trace), messageBundleKey, messageParams);
	}
	
	public TraceResult(CheckingStatus status, Trace trace) {
		this(status, trace, status.getTranslationKey());
	}
	
	@Override
	public List<Trace> getTraces() {
		return Collections.unmodifiableList(this.traces);
	}
	
	@Override
	public CheckingResult withoutAnimatorDependentState() {
		return new CheckingResult(this.getStatus(), this.getMessageBundleKey(), this.getMessageParams());
	}
}
