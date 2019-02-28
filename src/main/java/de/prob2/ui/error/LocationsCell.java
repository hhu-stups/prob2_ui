package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;

import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.VBox;

final class LocationsCell extends TreeTableCell<Object, Object> {
	@Inject
	private LocationsCell() {}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item instanceof String) {
			this.setGraphic(null);
		} else if (item instanceof ErrorItem) {
			final VBox vbox = new VBox();
			for (final ErrorItem.Location location : ((ErrorItem)item).getLocations()) {
				final StringBuilder sb = new StringBuilder();
				sb.append(location.getStartLine());
				sb.append(':');
				sb.append(location.getStartColumn());
				
				if (location.getStartLine() != location.getEndLine() || location.getStartColumn() != location.getEndColumn()) {
					sb.append(" to ");
					sb.append(location.getEndLine());
					sb.append(':');
					sb.append(location.getEndColumn());
				}
				
				vbox.getChildren().add(new Label(sb.toString()));
			}
			this.setGraphic(vbox);
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
