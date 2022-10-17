package de.prob2.ui.error;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.I18n;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.layout.VBox;

final class LocationsCell extends TreeTableCell<Object, Object> {
	private final Injector injector;

	@Inject
	private LocationsCell(Injector injector) {
		this.injector = injector;
	}

	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty || item instanceof String) {
			this.setGraphic(null);
		} else if (item instanceof ErrorItem) {
			I18n i18n = injector.getInstance(I18n.class);
			final List<ErrorItem.Location> locations = ((ErrorItem) item).getLocations();
			addContextMenu(locations);
			// this.getTreeTableRow() is deprecated since JavaFX 17
			// and the replacement this.getTableRow() was only introduced in JavaFX 17.
			// To stay compatible with older JavaFX versions without causing deprecation warnings on newer versions,
			// go through this.tableRowProperty() instead.
			final TreeTableRow<?> row = this.tableRowProperty().get();
			if (!locations.isEmpty()) {
				// If the TreeTableRow is double-clicked the BEditorView will jump to the first error location corresponding to the ErrorItem
				ErrorItem.Location firstError = locations.get(0);
				if (firstError.getFilename().isEmpty()) {
					// TODO Investigate the empty filenames and the corresponding locations given
					//  -(Empty filenames seems to appear with VisB models and show an error in the value part of the corresponding json)
					//  -(Check if other filetypes are involved and if not add line:column info accordingly)
					//  System.out.println(firstError.getStartLine()+":"+firstError.getStartColumn()+"-"+firstError.getEndLine()+":"+firstError.getEndColumn());
				} else {
					row.setOnMouseClicked(e -> {
						if (e.getClickCount() == 2) {
							jumpToResource(firstError);
						}
					});
					String lineAndColumn = i18n.translate("error.errorTable.columns.locations.line") + firstError.getStartLine() + ", " + i18n.translate("error.errorTable.columns.locations.column") + firstError.getStartColumn();
					String tooltipText = i18n.translate("error.errorTable.location.tooltip", lineAndColumn, firstError.getFilename());
					row.setTooltip(new Tooltip(tooltipText));
				}
			} else {
				row.setOnMouseClicked(null);
				row.setTooltip(null);
			}
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}

	private void addContextMenu(final List<ErrorItem.Location> locations) {
		I18n i18n = injector.getInstance(I18n.class);
		final VBox vbox = new VBox();
		for (final ErrorItem.Location location : locations) {
			final StringBuilder sb = new StringBuilder();
			sb.append(location.getStartLine());
			sb.append(":");
			sb.append(location.getStartColumn());

			if (location.getStartLine() != location.getEndLine() || location.getStartColumn() != location.getEndColumn()) {
				sb.append(i18n.translate("error.errorTable.columns.locations.to"));
				sb.append(location.getEndLine());
				sb.append(":");
				sb.append(location.getEndColumn());
			}
			// Add ContextMenu for every location to be able to jump to each of them
			if (!location.getFilename().isEmpty()) {
				ContextMenu contextMenu = new ContextMenu();
				MenuItem menuItem = new MenuItem(i18n.translate("error.errorTable.location.jumpTo",
						i18n.translate("error.errorTable.columns.locations.line") + location.getStartLine() + ", " + i18n.translate("error.errorTable.columns.locations.column") + location.getStartColumn()));
				menuItem.setOnAction(e -> jumpToResource(location));
				contextMenu.getItems().add(menuItem);

				Label locationLabel = new Label(sb.toString());
				locationLabel.setContextMenu(contextMenu);
				vbox.getChildren().add(locationLabel);
			}
		}
		this.setGraphic(vbox);
	}

	private void jumpToResource(ErrorItem.Location location) {
		BEditorView bEditorView = injector.getInstance(BEditorView.class);
		bEditorView.jumpToErrorSource(location);
	}
}
