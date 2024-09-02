package de.prob2.ui.verifications;

public class CheckingResult implements ICheckingResult {
	private final CheckingStatus status;
	private final String messageBundleKey;
	private final Object[] messageParams;
	
	public CheckingResult(CheckingStatus status, String messageBundleKey, Object... messageParams) {
		this.status = status;
		this.messageBundleKey = messageBundleKey;
		this.messageParams = messageParams;
	}
	
	public CheckingResult(CheckingStatus status) {
		this(status, status.getTranslationKey());
	}
	
	@Override
	public CheckingStatus getStatus() {
		return status;
	}
	
	@Override
	public String getMessageBundleKey() {
		return messageBundleKey;
	}
	
	@Override
	public Object[] getMessageParams() {
		return messageParams;
	}
	
	@Override
	public ICheckingResult withoutAnimatorDependentState() {
		return this;
	}
}
