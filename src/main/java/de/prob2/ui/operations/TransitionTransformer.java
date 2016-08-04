package de.prob2.ui.operations;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.util.Callback;

public class TransitionTransformer implements Callback<ListView<Operation>, ListCell<Operation>> {

	public class OperationsCell extends ListCell<Operation> {
		FontAwesomeIconView iconEnabled = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
		FontAwesomeIconView iconNotEnabled = new FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE);
		
		public OperationsCell() {
			iconEnabled.setFill(Color.LIMEGREEN);
			iconNotEnabled.setFill(Color.RED);
		}
		
		@Override
		protected void updateItem(Operation item, boolean empty) {
			super.updateItem(item, empty);
			if(item != null &! empty) {
				setText(item.toString());
				if(item.isEnabled()) {
					setGraphic(iconEnabled);
					setDisable(false);
				} else {
					setGraphic(iconNotEnabled);
					setDisable(true);
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