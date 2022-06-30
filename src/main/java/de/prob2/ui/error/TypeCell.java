package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.I18n;

import javafx.scene.control.TreeTableCell;

import static de.prob2.ui.internal.TranslatableAdapter.enumNameAdapter;

final class TypeCell extends TreeTableCell<Object, Object> {
	private final I18n i18n;

	@Inject
	private TypeCell(final I18n i18n) {
		this.i18n = i18n;
	}

	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty || item instanceof String) {
			this.setText(null);
		} else if (item instanceof ErrorItem) {
			this.setText(i18n.translate(enumNameAdapter("error.errorTable.type"), ((ErrorItem) item).getType()));
		} else {
			throw new AssertionError("Invalid table element type: " + item.getClass());
		}
	}
}
