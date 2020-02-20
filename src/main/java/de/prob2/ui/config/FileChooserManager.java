package de.prob2.ui.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.brules.RulesModelFactory;
import de.prob.scripting.AlloyFactory;
import de.prob.scripting.CSPFactory;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.scripting.FactoryProvider;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.TLAFactory;
import de.prob.scripting.XTLFactory;
import de.prob.scripting.ZFactory;
import de.prob2.ui.project.ProjectManager;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Singleton
public class FileChooserManager {
	public enum Kind {
		PROJECTS_AND_MACHINES, PLUGINS, VISUALISATIONS, PERSPECTIVES, TRACES, LTL
	}

	private static final Map<Class<? extends ModelFactory<?>>, String> FACTORY_TO_TYPE_KEY_MAP;
	static {
		final Map<Class<? extends ModelFactory<?>>, String> map = new HashMap<>();
		map.put(ClassicalBFactory.class, "common.fileChooser.fileTypes.classicalB");
		map.put(EventBFactory.class, "common.fileChooser.fileTypes.eventB");
		map.put(EventBPackageFactory.class, "common.fileChooser.fileTypes.eventBPackage");
		map.put(CSPFactory.class, "common.fileChooser.fileTypes.csp");
		map.put(TLAFactory.class, "common.fileChooser.fileTypes.tla");
		map.put(RulesModelFactory.class, "common.fileChooser.fileTypes.bRules");
		map.put(XTLFactory.class, "common.fileChooser.fileTypes.xtl");
		map.put(ZFactory.class, "common.fileChooser.fileTypes.z");
		map.put(AlloyFactory.class, "common.fileChooser.fileTypes.alloy");
		FACTORY_TO_TYPE_KEY_MAP = Collections.unmodifiableMap(map);
	}

	private final ResourceBundle bundle;

	private final List<FileChooser.ExtensionFilter> machineExtensionFilters;

	private final Map<Kind, Path> initialDirectories = new EnumMap<>(Kind.class);

	@Inject
	private FileChooserManager(final Config config, final ResourceBundle bundle) {
		this.bundle = bundle;

		this.machineExtensionFilters = new ArrayList<>();
		FactoryProvider.FACTORY_TO_EXTENSIONS_MAP.forEach((factory, extensions) -> {
			final String name;
			if (FACTORY_TO_TYPE_KEY_MAP.containsKey(factory)) {
				name = bundle.getString(FACTORY_TO_TYPE_KEY_MAP.get(factory));
			} else {
				name = factory.getSimpleName();
			}
			final List<String> extensionPatterns = extensions.stream().map(ext -> "*." + ext).collect(Collectors.toList());
			this.machineExtensionFilters.add(new FileChooser.ExtensionFilter(
				String.format(name, String.join(", ", extensionPatterns)),
				extensionPatterns
			));
		});
		this.machineExtensionFilters.sort(Comparator.comparing(FileChooser.ExtensionFilter::getDescription));

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.fileChooserInitialDirectories != null) {
					// Gson represents unknown kinds (from older or newer configs) as null.
					// We simply remove them here, because there's nothing meaningful we can do with them.
					configData.fileChooserInitialDirectories.remove(null);
					getInitialDirectories().clear();
					getInitialDirectories().putAll(configData.fileChooserInitialDirectories);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.fileChooserInitialDirectories = new EnumMap<>(Kind.class);
				configData.fileChooserInitialDirectories.putAll(getInitialDirectories());
			}
		});
	}

	public Path showOpenFileChooser(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}
		final File file = fileChooser.showOpenDialog(window);
		final Path path = file == null ? null : file.toPath();
		if (path != null) {
			setInitialDirectory(kind, path.getParent());
		}
		return path;
	}

	public Path showSaveFileChooser(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}
		final File file = fileChooser.showSaveDialog(window);
		final Path path = file == null ? null : file.toPath();
		if (path != null) {
			setInitialDirectory(kind, path.getParent());
		}
		return path;
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB file.
	 * 
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @param projects whether projects should be selectable
	 * @param machines whether machines should be selectable
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	private Path showOpenProjectOrMachineChooser(final Window window, final boolean projects, final boolean machines) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("common.fileChooser.open.title"));
		
		final List<String> allExts = new ArrayList<>();
		if (projects) {
			allExts.add(ProjectManager.PROJECT_FILE_PATTERN);
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(String.format(bundle.getString("common.fileChooser.fileTypes.proB2Project"), ProjectManager.PROJECT_FILE_PATTERN), ProjectManager.PROJECT_FILE_PATTERN));
			allExts.sort(String::compareTo);
		}
		
		if (machines) {
			allExts.addAll(FactoryProvider.EXTENSION_PATTERNS_ORDERED);
			fileChooser.getExtensionFilters().addAll(this.machineExtensionFilters);
		}

		fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.allProB"), allExts));
		return this.showOpenFileChooser(fileChooser, FileChooserManager.Kind.PROJECTS_AND_MACHINES, window);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB 2 project.
	 * 
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenProjectChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, true, false);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenMachineChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, false, true);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB 2 project or a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenProjectOrMachineChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, true, true);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to save a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showSaveMachineChooser(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("common.fileChooser.save.title"));
		
		final List<String> allExts = new ArrayList<>(FactoryProvider.EXTENSION_PATTERNS_ORDERED);
		fileChooser.getExtensionFilters().addAll(this.machineExtensionFilters);

		fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.allProB"), allExts));
		return this.showSaveFileChooser(fileChooser, FileChooserManager.Kind.PROJECTS_AND_MACHINES, window);
	}

	private boolean containsValidInitialDirectory(Kind kind) {
		return initialDirectories.containsKey(kind) && Files.exists(initialDirectories.get(kind));
	}

	private Path getInitialDirectory(Kind kind) {
		return this.initialDirectories.get(kind);
	}

	private void setInitialDirectory(Kind kind, Path dir) {
		if (dir != null && Files.exists(dir)) {
			initialDirectories.put(kind, dir);
		}
	}

	private Map<Kind, Path> getInitialDirectories() {
		return initialDirectories;
	}
}
