package de.prob2.ui.operations;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class TransitionTransformer implements Callback<ListView<Operation>, ListCell<Operation>> {

	public class OperationsCell extends ListCell<Operation> {
		ImageView imEnabled = new ImageView(
				new Image(getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-174-play.png")));
		ImageView imNotEnabled = new ImageView(
				new Image(getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-200-ban-circle.png")));
		
		public OperationsCell() {
			Lighting lighting = new Lighting();
			lighting.setSpecularConstant(1.5);
			lighting.setSpecularExponent(0.0);
			ColorAdjust green = new ColorAdjust();
			green.setInput(lighting);
			green.setHue(0.6);
			green.setSaturation(1);
			imEnabled.setEffect(green);
			imEnabled.setFitWidth(13);
			imEnabled.setFitHeight(13);
			ColorAdjust red = new ColorAdjust();
			red.setInput(lighting);
			red.setHue(0.0);
			red.setSaturation(1);
			imNotEnabled.setEffect(red);
			imNotEnabled.setFitWidth(13);
			imNotEnabled.setFitHeight(13);
		}
		
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