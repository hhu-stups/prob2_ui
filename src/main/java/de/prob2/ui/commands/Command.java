package de.prob2.ui.commands;

import java.util.Collection;
import java.util.Map;

import de.prob2.ui.modeline.ModelineExtension;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;

public interface Command {

	void populateMenuBar(MenuBar menuBar, Map<String, Menu> menus);

	Collection<ModelineExtension> getModelineContribution();

}
