package de.prob2.ui.dynamic;


import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Cell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesCellProvider;
import de.prob2.ui.preferences.ProBPreferences;

public class DynamicTableCell extends TableCell<PrefItem, String> {
	
	private final PreferencesCellProvider<? extends Cell<String>, TableRow<PrefItem>> provider;
	
	public DynamicTableCell(final ReadOnlyObjectProperty<ProBPreferences> preferences) {
		super();
		this.provider = new PreferencesCellProvider<>(this, preferences);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		provider.setRow(this.getTableRow());
		provider.updateItem(item);
	}
}
