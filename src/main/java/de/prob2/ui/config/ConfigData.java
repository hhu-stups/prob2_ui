package de.prob2.ui.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.operations.OperationsView;

import javafx.geometry.BoundingBox;

/**
 * The full set of config settings, used when the injector is available.
 */
@SuppressWarnings("PublicField")
public final class ConfigData extends BasicConfigData {
	public int maxRecentProjects;
	public int fontSize;
	public List<String> recentProjects;
	public Console.ConfigData groovyConsoleSettings;
	public Console.ConfigData bConsoleSettings;
	public String guiState;
	public List<String> visibleStages;
	public Map<String, BoundingBox> stageBoxes;
	public String currentPreference;
	public String currentMainTab;
	public String currentVerificationTab;
	public List<String> expandedTitledPanes;
	public boolean bConsoleExpanded;
	public String defaultProjectLocation;
	public double[] horizontalDividerPositions;
	public double[] verticalDividerPositions;
	public double[] statesViewColumnsWidth;
	public String[] statesViewColumnsOrder;
	public OperationsView.SortMode operationsSortMode;
	public boolean operationsShowNotEnabled;
	public Map<String, String> globalPreferences;
	public Path pluginDirectory;
	public Map<FileChooserManager.Kind, Path> fileChooserInitialDirectories;
	
	ConfigData() {}
}
