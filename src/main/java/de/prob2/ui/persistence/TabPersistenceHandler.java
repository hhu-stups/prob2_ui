package de.prob2.ui.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TabPersistenceHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TabPersistenceHandler.class);
	
	private TabPersistenceHandler() {
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
}
