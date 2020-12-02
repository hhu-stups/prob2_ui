package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeTableCell;
import javafx.scene.text.Text;

final class MessageCell extends TreeTableCell<Object, Object> {
	@Inject
	private MessageCell() {}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			this.setText(null);
			this.setGraphic(null);
		} else if (item instanceof String) {
			this.setText((String)item);
			this.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
			this.setGraphic(null);
			// TODO Wrap text here to?
			/*Text text = new Text((String) item);
			text.wrappingWidthProperty().bind(this.getTableColumn().widthProperty());
			this.setWrapText(true);
			this.setGraphic(text);*/
		} else if (item instanceof ErrorItem) {
			/*this.setText(((ErrorItem)item).getMessage());
			this.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);*/
			this.setText(null);
			Text text = new Text(((ErrorItem) item).getMessage());
			text.wrappingWidthProperty().bind(this.getTableColumn().widthProperty().subtract(5));
			this.setWrapText(true);
			this.setGraphic(text);
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
