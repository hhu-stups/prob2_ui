package de.prob2.ui.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PersistenceUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceUtils.class);
	
	private PersistenceUtils() {
		throw new AssertionError("Utility class");
	}
	
	public static String getCurrentTab(final TabPane pane) {
		return pane.getSelectionModel().getSelectedItem().getId();
	}
	
	public static void setCurrentTab(final TabPane pane, final String id) {
		final Optional<Tab> foundTab = pane.getTabs().stream()
			.filter(tab -> tab.getId().equals(id))
			.findAny();
		
		if (foundTab.isPresent()) {
			pane.getSelectionModel().select(foundTab.get());
		} else {
			final List<String> availableIds = pane.getTabs().stream()
				.map(Tab::getId)
				.collect(Collectors.toList());
			LOGGER.warn("Could not find tab with ID {} (from available IDs {}) - not restoring selected tab", id, availableIds);
		}
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
