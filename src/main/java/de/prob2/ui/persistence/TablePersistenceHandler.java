package de.prob2.ui.persistence;

import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;
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

	public <S> void setColumnsWidth(TreeTableView<S> tableView, ObservableList<TreeTableColumn<S, ?>> columns) {
		double[] widths = uiState.getStatesViewColumnsWidth();
		double sum = 0.0;
		for (final TableColumnBase<?, ?> column : columns) {
			sum += column.getWidth();
		}

		for (int i = 0; i < columns.size() - 1; i++) {
			tableView.resizeColumn(columns.get(i), widths[i] * sum - columns.get(i).getWidth());
		}
	}

	public <S> void setColumnsOrder(ObservableList<TreeTableColumn<S, ?>> columns) {
		ObservableList<TreeTableColumn<S, ?>> newColumns = FXCollections.observableArrayList();
		for (final String text : uiState.getStatesViewColumnsOrder()) {
			newColumns.addAll(columns.stream().filter(column -> column.getId().equals(text)).collect(Collectors.toList()));
		}

		columns.stream().filter(column -> !newColumns.contains(column)).forEach(newColumns::add);
		columns.setAll(newColumns);
	}

	public String[] getColumnsOrder(List<? extends TableColumnBase<?, ?>> columns) {
		String[] order = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			order[i] = columns.get(i).getId();
		}
		return order;
	}
}
