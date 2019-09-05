package de.prob2.ui.sharedviews;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;

public class WrappedTextTableCell<T> extends TableCell<T, String> {
	
	private final TableColumn<T, String> column;
	
	public WrappedTextTableCell(final TableColumn<T, String> column) {
		this.column = column;
	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		if (!isEmpty()) {
			Text text = new Text(item);
			text.wrappingWidthProperty().bind(column.widthProperty());
			this.setWrapText(true);
			this.setGraphic(text);
		}
	}
	
}
