package de.prob2.ui.verifications.ltl;

import javax.annotation.Nullable;

import de.prob.statespace.Trace;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

public class LTLResultHandler {
	
	public enum Checked {
		SUCCESS, FAIL;
	}
	
	public static class LTLResultItem {
	
		private AlertType type;
		private Checked checked;
		private String message;
		private String header;
		private String exceptionText;
		private boolean isParseError;
		
		public LTLResultItem(AlertType type, Checked checked, String message, String header) {
			this.type = type;
			this.checked = checked;
			this.message = message;
			this.header = header;
		}
		
		public LTLResultItem(AlertType type, Checked checked, String message, String header, 
								String exceptionText) {
			this(type, checked, message, header);
			this.exceptionText = exceptionText;
			this.isParseError = true;
		}
		
		public Checked getChecked() {
			return checked;
		}
	
	}
	
	public void showResult(LTLResultItem resultItem, LTLCheckableItem item, @Nullable Trace trace) {
		if(resultItem == null) {
			return;
		}
		Alert alert = new Alert(resultItem.type, resultItem.message);
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.header);
		if(resultItem.isParseError) {
			alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
			TextArea exceptionText = new TextArea(resultItem.exceptionText);
			exceptionText.setEditable(false);
			exceptionText.getStyleClass().add("text-area-error");
			StackPane pane = new StackPane(exceptionText);
			pane.setPrefSize(320, 120);
			alert.getDialogPane().setExpandableContent(pane);
			alert.getDialogPane().setExpanded(true);
		}
		alert.showAndWait();
		if(resultItem.type != AlertType.ERROR) {
			item.setCheckedSuccessful();
		} else {
			item.setCheckedFailed();
		}
		if(item instanceof LTLFormulaItem) {
			((LTLFormulaItem) item).setCounterExample(trace);
		}
	}
}