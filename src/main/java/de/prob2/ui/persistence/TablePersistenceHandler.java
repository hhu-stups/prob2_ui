package de.prob2.ui.persistence;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

@Singleton
public final class TablePersistenceHandler {
	private final UIState uiState;

	@Inject
	private TablePersistenceHandler(UIState uiState) {
		this.uiState = uiState;
	}

	public double[] getColumnsWidth(List<? extends TableColumnBase<?, ?>> columns) {
		double[] result = new double[columns.size()];
		double sum = 0.0;
		for (int i = 0; i < columns.size(); i++) {
			result[i] = columns.get(i).getWidth();
			sum += result[i];
		}
		for (int i = 0; i < result.length; i++) {
			result[i] /= sum;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public void setColumnsWidth(Control control, ObservableList<? extends TableColumnBase<?, ?>> columns) {
		if (!(control instanceof TreeTableView || control instanceof TableView)) {
			return;
		}

		double[] widths = uiState.getStatesViewColumnsWidth();
		double sum = 0.0;
		for (final TableColumnBase<?, ?> column : columns) {
			sum += column.getWidth();
		}

		for (int i = 0; i < columns.size() - 1; i++) {
			((TreeTableView<Object>) control).resizeColumn((TreeTableColumn<Object, ?>) columns.get(i),
					widths[i] * sum - columns.get(i).getWidth());
		}
	}

	@SuppressWarnings("unchecked")
	public void setColumnsOrder(ObservableList<? extends TableColumnBase<?, ?>> columns) {
		ObservableList<TableColumnBase<?, ?>> newColumns = FXCollections.observableArrayList();
		for (final String text : uiState.getStatesViewColumnsOrder()) {
			for (TableColumnBase<?, ?> column : columns) {
				if (column.getId().equals(text)) {
					newColumns.add(column);
				}
			}
		}

		for (final TableColumnBase<?, ?> column : columns) {
			if (!newColumns.contains(column)) {
				newColumns.add(column);
			}
		}

		columns.clear();
		((ObservableList<TableColumnBase<?, ?>>) columns).addAll(newColumns);
	}

	public String[] getColumnsOrder(List<? extends TableColumnBase<?, ?>> columns) {
		String[] order = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			order[i] = columns.get(i).getId();
		}
		return order;
	}
}
