package de.prob2.ui.verifications;

public class CheckingResultItem {
	
	private Checked checked;
	private String headerBundleKey;
	private String messageBundleKey;
	private Object[] messageParams;
	
	public CheckingResultItem(Checked checked, String headerBundleKey, String messageBundleKey, Object... messageParams) {
		this.checked = checked;
		this.headerBundleKey = headerBundleKey;
		this.messageBundleKey = messageBundleKey;
		this.messageParams = messageParams;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getHeaderBundleKey() {
		return headerBundleKey;
	}
	
	public String getMessageBundleKey() {
		return messageBundleKey;
	}
	
	public Object[] getMessageParams() {
		return messageParams;
	}
}