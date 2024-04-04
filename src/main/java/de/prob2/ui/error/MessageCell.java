package de.prob2.ui.error;

import com.google.inject.Inject;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.ErrorItem;

import de.prob2.ui.internal.I18n;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.FileSystems;

final class MessageCell extends TreeTableCell<Object, Object> {
	private final Injector injector;
	@Inject
	private MessageCell(Injector injector) {
		this.injector = injector;
	}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			this.setText(null);
			this.setGraphic(null);
		} else if (item instanceof String) {
			if (((String) item).isEmpty()) {
				// TODO Check if this occurs with other file types
				//  this.setText("(VisB json error)");
			} else {
				this.setText(((String) item).substring(((String) item).lastIndexOf(FileSystems.getDefault().getSeparator())+1));
				final TreeTableRow<?> row = this.tableRowProperty().get();
				// Set tooltip for full filename
				row.setTooltip(new Tooltip((String) item));
			}
			this.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
			this.setGraphic(null);
		} else if (item instanceof ErrorItem) {
			this.setText(null);
			String errorItemMessageContent = ((ErrorItem) item).getMessage();
			Text text = new Text(errorItemMessageContent);
			text.wrappingWidthProperty().bind(this.getTableColumn().widthProperty().subtract(5));
			this.setWrapText(true);
			this.setGraphic(text);
			//Add ContextMenu to MessageCell for copying messages
			MenuItem mi = new MenuItem(injector.getInstance(I18n.class).translate("error.errorTable.message.copy"));
			mi.setOnAction(e -> {
				ClipboardContent cc = new ClipboardContent();
				cc.putString(errorItemMessageContent);
				Clipboard.getSystemClipboard().setContent(cc);
			});
			this.setContextMenu(new ContextMenu(mi));
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
