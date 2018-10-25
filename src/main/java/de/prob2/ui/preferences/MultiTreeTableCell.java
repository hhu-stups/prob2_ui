package de.prob2.ui.preferences;

import com.google.inject.Injector;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Cell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;


public class MultiTreeTableCell extends TreeTableCell<PrefItem, String> {
	
	private final PreferencesCellProvider<? extends Cell<String>, TreeTableRow<PrefItem>> provider;
	
	public MultiTreeTableCell(final ReadOnlyObjectProperty<ProBPreferences> preferences, final Injector injector) {
		super();
		this.provider = new PreferencesCellProvider<>(this, injector, preferences);
	}
	
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		provider.setRow(this.getTreeTableRow());
		provider.updateItem(item);
	}
}
