package de.prob2.ui.operations;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class TransitionTransformer implements Callback<ListView<Operation>, ListCell<Operation>> {

	public class OperationsCell extends ListCell<Operation> {
		ImageView imEnabled = new ImageView(
				new Image(getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-174-play.png")));
		ImageView imNotEnabled = new ImageView(
				new Image(getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-200-ban-circle.png")));
		
		@Override
		protected void updateItem(Operation item, boolean empty) {
			super.updateItem(item, empty);
			if(item != null &! empty) {
				setText(item.toString());
				if(item.isEnabled()) {
					setGraphic(imEnabled);
				} else {
					setGraphic(imNotEnabled);
				}
			} else {
				setGraphic(null);
				setText(null);
			}
		}
	}

	@Override
	public ListCell<Operation> call(ListView<Operation> lv) {
		return new OperationsCell();
	}
}