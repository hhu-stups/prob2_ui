package de.prob2.ui.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

public final class TableUtils {
	private TableUtils() {
		throw new AssertionError("Utility class");
	}

	public static List<Double> getAbsoluteColumnWidths(List<? extends TableColumnBase<?, ?>> columns) {
		// noinspection Convert2MethodRef // Using a method reference causes a raw type warning.
		return columns.stream()
			.map(TableColumnBase::getWidth)
			.collect(Collectors.toList());
	}
	
	public static <S> void setAbsoluteColumnWidths(TreeTableView<S> tableView, List<TreeTableColumn<S, ?>> columns, List<Double> widths) {
		for (int i = 0; i < columns.size(); i++) {
			tableView.resizeColumn(columns.get(i), widths.get(i) - columns.get(i).getWidth());
		}
	}

	public static List<Double> getRelativeColumnWidths(List<? extends TableColumnBase<?, ?>> columns) {
		// noinspection Convert2MethodRef // Using a method reference causes a raw type warning.
		final double sum = columns.stream().mapToDouble(TableColumnBase::getWidth).sum();
		return columns.stream()
			.map(col -> col.getWidth() / sum)
			.collect(Collectors.toList());
	}

	public static <S> void setRelativeColumnWidths(TreeTableView<S> tableView, List<TreeTableColumn<S, ?>> columns, List<Double> widths) {
		final double sum = columns.stream().mapToDouble(TableColumnBase::getWidth).sum();
		for (int i = 0; i < columns.size() - 1; i++) {
			tableView.resizeColumn(columns.get(i), widths.get(i) * sum - columns.get(i).getWidth());
		}
	}

	public static <S> void setColumnsOrder(ObservableList<TreeTableColumn<S, ?>> columns, final List<String> order) {
		final List<TreeTableColumn<S, ?>> newColumns = new ArrayList<>();
		for (final String text : order) {
			newColumns.addAll(columns.stream().filter(column -> column.getId().equals(text)).collect(Collectors.toList()));
		}

		columns.stream().filter(column -> !newColumns.contains(column)).forEach(newColumns::add);
		columns.setAll(newColumns);
	}

	public static List<String> getColumnsOrder(List<? extends TableColumnBase<?, ?>> columns) {
		// noinspection Convert2MethodRef // Using a method reference causes a raw type warning.
		return columns.stream()
			.map(TableColumnBase::getId)
			.collect(Collectors.toList());
	}
}
