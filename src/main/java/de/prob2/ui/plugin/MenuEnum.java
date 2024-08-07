package de.prob2.ui.plugin;

public enum MenuEnum {
	FILE_MENU("fileMenu"),
	VIEW_MENU("viewMenu"),
	VISUALISATION_MENU("visualisationMenu"),
	ADVANCED_MENU("pluginMenu"),
	WINDOW_MENU("windowMenu"),
	HELP_MENU("helpMenu"),
	VISUALISATION_FX_MENU("visualisationFxMenu"),
	RECENT_PROJECTS_MENU("recentProjectsMenu"),
	PRESET_PERSPECTIVES_MENU("presetPerspectivesMenu");

	private final String id;

	MenuEnum(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}
}
