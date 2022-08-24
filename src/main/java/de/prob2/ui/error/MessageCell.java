package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeTableCell;
import javafx.scene.text.Text;

import java.nio.file.FileSystems;

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
			this.setText(((String) item).substring(((String) item).lastIndexOf(FileSystems.getDefault().getSeparator())+1));
			this.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
			this.setGraphic(null);
		} else if (item instanceof ErrorItem) {
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
