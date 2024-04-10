package de.prob2.ui.error;

import java.nio.file.FileSystems;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.I18n;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;

final class MessageCell extends TreeTableCell<Object, Object> {

	private final Injector injector;

	@Inject
	private MessageCell(Injector injector) {
		this.injector = injector;
	}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty || item == null) {
			this.setText(null);
			this.setGraphic(null);
		} else if (item instanceof String s) {
			if (s.isEmpty()) {
				// TODO: Check if this occurs with other file types
				// this.setText("(VisB json error)");
				this.setText("");
			} else {
				this.setText(s.substring(s.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1));
				// Set tooltip for full filename
				final TreeTableRow<?> row = this.tableRowProperty().get();
				row.setTooltip(new Tooltip((String) item));
			}
			this.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
			this.setGraphic(null);
		} else if (item instanceof ErrorItem) {
			this.setText(null);
			String errorItemMessageContent = ((ErrorItem) item).getMessage();
			Text text = new Text(errorItemMessageContent);
			text.wrappingWidthProperty().bind(this.getTableColumn().widthProperty().subtract(5));
			this.setGraphic(text);

			// Add ContextMenu for copying messages
			MenuItem mi = new MenuItem(injector.getInstance(I18n.class).translate("common.buttons.copyToClipboard"));
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
