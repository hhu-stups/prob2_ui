package de.prob2.ui.dynamic;

import com.google.inject.Injector;

import de.prob2.ui.preferences.PrefItem;
import de.prob2.ui.preferences.PreferencesCellProvider;
import de.prob2.ui.preferences.ProBPreferences;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Cell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

public class DynamicTableCell extends TableCell<PrefItem, String> {
	
	private final PreferencesCellProvider<? extends Cell<String>, TableRow<PrefItem>> provider;
	
	public DynamicTableCell(final ReadOnlyObjectProperty<ProBPreferences> preferences, final Injector injector) {
		super();
		this.provider = new PreferencesCellProvider<>(this, injector, preferences);
	}

	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);
		// getTableRow() returns a raw type in JDK 8, this is fixed in later versions.
		@SuppressWarnings({"unchecked", "RedundantSuppression"})
		final TableRow<PrefItem> row = this.getTableRow();
		provider.setRow(row);
		provider.updateItem(item);
	}
}
