package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.TableCell;

final class MessageCell extends TableCell<ErrorItem, ErrorItem> {
	@Inject
	private MessageCell() {}
	
	@Override
	protected void updateItem(final ErrorItem item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item == null) {
			this.setText(null);
		} else {
			this.setText(item.getMessage());
		}
	}
}
