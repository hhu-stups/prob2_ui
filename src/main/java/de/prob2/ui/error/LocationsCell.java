package de.prob2.ui.error;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.beditor.BEditorView;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class LocationsCell extends TableCell<ErrorItem, ErrorItem> {
	private final Provider<BEditorView> bEditorViewProvider;
	
	@Inject
	private LocationsCell(final Provider<BEditorView> bEditorViewProvider) {
		this.bEditorViewProvider = bEditorViewProvider;
	}
	
	@Override
	protected void updateItem(final ErrorItem item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item == null) {
			this.setGraphic(null);
		} else {
			final VBox vbox = new VBox();
			for (final ErrorItem.Location location : item.getLocations()) {
				final Button openLocationButton = new Button(null, new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
				openLocationButton.setOnAction(event -> {
					this.bEditorViewProvider.get().selectRange(
						location.getStartLine()-1, location.getStartColumn(),
						location.getEndLine()-1, location.getEndColumn()
					);
				});
				final Label label = new Label(location.toString());
				label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
				final HBox hbox = new HBox(openLocationButton, label);
				HBox.setHgrow(openLocationButton, Priority.NEVER);
				HBox.setHgrow(label, Priority.ALWAYS);
				hbox.setAlignment(Pos.CENTER_LEFT);
				vbox.getChildren().add(hbox);
			}
			this.setGraphic(vbox);
		}
	}
}
