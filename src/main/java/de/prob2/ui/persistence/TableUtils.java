package de.prob2.ui.persistence;

import java.util.List;
import java.util.stream.Collectors;

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
			.map(c -> c.getWidth())
			.collect(Collectors.toList());
	}
	
	public static <S> void setAbsoluteColumnWidths(TreeTableView<S> tableView, List<TreeTableColumn<S, ?>> columns, List<Double> widths) {
		for (int i = 0; i < columns.size(); i++) {
			tableView.resizeColumn(columns.get(i), widths.get(i) - columns.get(i).getWidth());
		}
	}
}
