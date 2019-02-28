package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeTableCell;

final class MessageCell extends TreeTableCell<Object, Object> {
	@Inject
	private MessageCell() {}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty) {
			this.setText(null);
		} else if (item instanceof String) {
			this.setText((String)item);
			this.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
		} else if (item instanceof ErrorItem) {
			this.setText(((ErrorItem)item).getMessage());
			this.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
