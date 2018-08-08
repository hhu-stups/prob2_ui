package de.prob2.ui.verifications;

public class CheckingResultItem {
	
	private Checked checked;
	private String message;
	private String header;
	
	public CheckingResultItem(Checked checked, String message, String header) {
		this.checked = checked;
		this.message = message;
		this.header = header;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getHeader() {
		return header;
	}
}