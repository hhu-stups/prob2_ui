package de.prob2.ui.error;

import com.google.inject.Inject;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.ErrorItem;

import de.prob2.ui.beditor.BEditor;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.ResourceBundle;

final class LocationsCell extends TreeTableCell<Object, Object> {
	final private Injector injector;

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
			final VBox vbox = new VBox();
			for (final ErrorItem.Location location : ((ErrorItem)item).getLocations()) {
				final StringBuilder sb = new StringBuilder();
				ResourceBundle rb = injector.getInstance(ResourceBundle.class);
				sb.append(rb.getString("error.errorTable.columns.locations.line") + location.getStartLine());
				sb.append(", ");
				sb.append(rb.getString("error.errorTable.columns.locations.column") + location.getStartColumn());
				
				if (location.getStartLine() != location.getEndLine() || location.getStartColumn() != location.getEndColumn()) {
					sb.append(rb.getString("error.errorTable.columns.locations.to"));
					sb.append(rb.getString("error.errorTable.columns.locations.line") + location.getEndLine());
					sb.append(", ");
					sb.append(rb.getString("error.errorTable.columns.locations.column") + location.getEndColumn());
				}
				// Add ContextMenu for every location to be able to jump to each of them
				ContextMenu contextMenu = new ContextMenu();
				MenuItem menuItem = new MenuItem(String.format(rb.getString("error.errorTable.location.jumpTo"),
						rb.getString("error.errorTable.columns.locations.line") + location.getStartLine() + ", " +
						rb.getString("error.errorTable.columns.locations.column") + location.getStartColumn()));
				menuItem.setOnAction(e -> jumpToResource(location));
				contextMenu.getItems().add(menuItem);

				Label locationLabel = new Label(sb.toString());
				locationLabel.setContextMenu(contextMenu);
				vbox.getChildren().add(locationLabel);
			}
			this.setGraphic(vbox);
			// If the TreeTableRow is clicked the BEditorView will jump to the first error corresponding to the file
			this.getTreeTableRow().setOnMouseClicked(e -> jumpToResource(((ErrorItem)item).getLocations().get(0)));
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}

	private void jumpToResource(ErrorItem.Location location) {
		((Stage) this.getScene().getWindow()).toBack();
		injector.getInstance(StageManager.class).getMainStage().toFront();
		injector.getInstance(MainView.class).switchTabPane("beditorTab");
		injector.getInstance(BEditorView.class).selectMachine(new File(location.getFilename()).toPath());
		BEditor bEditor = injector.getInstance(BEditor.class);
		bEditor.requestFocus();
		bEditor.moveTo(bEditor.getAbsolutePosition(location.getStartLine()-1, location.getStartColumn()));
		bEditor.requestFollowCaret();
	}
}
