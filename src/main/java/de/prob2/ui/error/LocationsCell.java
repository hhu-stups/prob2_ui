package de.prob2.ui.error;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.beditor.BEditor;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

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
			I18n i18n = injector.getInstance(I18n.class);
			final List<ErrorItem.Location> locations = ((ErrorItem)item).getLocations();
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
				ContextMenu contextMenu = new ContextMenu();
				MenuItem menuItem = new MenuItem(i18n.translate("error.errorTable.location.jumpTo",
						i18n.translate("error.errorTable.columns.locations.line") + location.getStartLine() + ", " + i18n.translate("error.errorTable.columns.locations.column") + location.getStartColumn()));
				menuItem.setOnAction(e -> jumpToResource(location));
				contextMenu.getItems().add(menuItem);

				Label locationLabel = new Label(sb.toString());
				locationLabel.setContextMenu(contextMenu);
				vbox.getChildren().add(locationLabel);
			}
			this.setGraphic(vbox);
			// this.getTreeTableRow() is deprecated since JavaFX 17
			// and the replacement this.getTableRow() was only introduced in JavaFX 17.
			// To stay compatible with older JavaFX versions without causing deprecation warnings on newer versions,
			// go through this.tableRowProperty() instead.
			final TreeTableRow<?> row = this.tableRowProperty().get();
			if (!locations.isEmpty()) {
				// If the TreeTableRow is double clicked the BEditorView will jump to the first error location corresponding to the ErrorItem
				ErrorItem.Location firstError = locations.get(0);
				row.setOnMouseClicked(e -> {
					if (e.getClickCount() == 2) {
						jumpToResource(firstError);
					}
				});
				String lineAndColumn = i18n.translate("error.errorTable.columns.locations.line") + firstError.getStartLine() + ", " + i18n.translate("error.errorTable.columns.locations.column") + firstError.getStartColumn();
				String tooltipText = i18n.translate("error.errorTable.location.tooltip", lineAndColumn, firstError.getFilename());
				row.setTooltip(new Tooltip(tooltipText));
			} else {
				row.setOnMouseClicked(null);
				row.setTooltip(null);
			}
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}

	private static <T> Collection<T> combineCollections(final Collection<T> a, final Collection<T> b) {
		final Collection<T> ret = new ArrayList<>(a);
		ret.addAll(b);
		return ret;
	}

	private void jumpToResource(ErrorItem.Location location) {
		injector.getInstance(StageManager.class).getMainStage().toFront();
		injector.getInstance(MainView.class).switchTabPane("beditorTab");

		BEditorView bEditorView = injector.getInstance(BEditorView.class);
		bEditorView.selectMachine(new File(location.getFilename()).toPath());

		BEditor bEditor = injector.getInstance(BEditor.class);
		// Remove the error markers set before this
		bEditor.resetHighlighting();
		int start = bEditor.getAbsolutePosition(location.getStartLine()-1, location.getStartColumn());
		int end = bEditor.getAbsolutePosition(location.getEndLine()-1, location.getEndColumn());
		StyleSpans<Collection<String>> errorStyleSpans = bEditor.getStyleSpans(0, end);
		errorStyleSpans = errorStyleSpans.overlay(new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), start).add(Collections.singleton("errorTable"), end-start).create(), LocationsCell::combineCollections);
		bEditor.setStyleSpans(0, errorStyleSpans);

		bEditor.requestFocus();
		bEditor.moveTo(bEditor.getAbsolutePosition(location.getStartLine()-1, location.getStartColumn()));
		bEditor.requestFollowCaret();
	}
}
