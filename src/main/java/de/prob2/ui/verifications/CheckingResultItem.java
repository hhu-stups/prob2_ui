package de.prob2.ui.verifications;

import javafx.scene.control.Alert.AlertType;

public class CheckingResultItem {
	
	private AlertType type;
	private Checked checked;
	private String message;
	private String header;
	private String exceptionText;
	
	public CheckingResultItem(AlertType type, Checked checked, String message, String header) {
		this.type = type;
		this.checked = checked;
		this.message = message;
		this.header = header;
	}
	
	public CheckingResultItem(AlertType type, Checked checked, String message, String header, 
							String exceptionText) {
		this(type, checked, message, header);
		this.exceptionText = exceptionText;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public AlertType getType() {
		return type;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getHeader() {
		return header;
	}
	
	public String getExceptionText() {
		return exceptionText;
	}

}