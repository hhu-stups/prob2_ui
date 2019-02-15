package de.prob2.ui.error;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.TableCell;

final class TypeCell extends TableCell<ErrorItem, ErrorItem> {
	private final ResourceBundle bundle;
	
	@Inject
	private TypeCell(final ResourceBundle bundle) {
		this.bundle = bundle;
	}
	
	@Override
	protected void updateItem(final ErrorItem item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item == null) {
			this.setText(null);
		} else {
			final String typeName;
			switch (item.getType()) {
				case WARNING:
					typeName = bundle.getString("error.exceptionAlert.proBErrorTable.type.warning");
					break;
				
				case ERROR:
					typeName = bundle.getString("error.exceptionAlert.proBErrorTable.type.error");
					break;
				
				case INTERNAL_ERROR:
					typeName = bundle.getString("error.exceptionAlert.proBErrorTable.type.internalError");
					break;
				
				default:
					typeName = item.getType().name();
			}
			this.setText(typeName);
		}
	}
}
