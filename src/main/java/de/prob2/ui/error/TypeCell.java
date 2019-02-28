package de.prob2.ui.error;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.TreeTableCell;

final class TypeCell extends TreeTableCell<Object, Object> {
	private final ResourceBundle bundle;
	
	@Inject
	private TypeCell(final ResourceBundle bundle) {
		this.bundle = bundle;
	}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item instanceof String) {
			this.setText(null);
		} else if (item instanceof ErrorItem) {
			final String typeName;
			switch (((ErrorItem)item).getType()) {
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
					typeName = ((ErrorItem)item).getType().name();
			}
			this.setText(typeName);
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
