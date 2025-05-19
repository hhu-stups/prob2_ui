package de.prob2.ui.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.MoreFiles;
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
import de.prob.scripting.ZFuzzFactory;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

@Singleton
public final class FileChooserManager {
	public enum Kind {
		PROJECTS_AND_MACHINES, NEW_MACHINE, PLUGINS, VISUALISATIONS, PERSPECTIVES, TRACES, SIMULATION, HISTORY_CHART
	}

	public static final String EXTENSION_PATTERN_PREFIX = "*.";
	private static final Map<Class<? extends ModelFactory<?>>, String> FACTORY_TO_TYPE_KEY_MAP;
	private static final Map<Class<? extends ModelFactory<?>>, String> NEW_MACHINE_FACTORY_TO_TYPE_KEY_MAP;
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
		map.put(ZFuzzFactory.class, "common.fileChooser.fileTypes.zFuzz");
		map.put(AlloyFactory.class, "common.fileChooser.fileTypes.alloy");
		FACTORY_TO_TYPE_KEY_MAP = Map.copyOf(map);

		// Remove unsupported file types for creating new machines
		map.remove(EventBFactory.class);
		map.remove(EventBPackageFactory.class);
		//map.remove(ZFactory.class);
		map.remove(ZFuzzFactory.class);
		NEW_MACHINE_FACTORY_TO_TYPE_KEY_MAP = Map.copyOf(map);
	}

	private final I18n i18n;
	private final StageManager stageManager;

	private final List<String> machineExtensionPatterns;
	private final List<FileChooser.ExtensionFilter> machineExtensionFilters;

	private final Map<Kind, Path> initialDirectories = new EnumMap<>(Kind.class);

	@Inject
	private FileChooserManager(final Config config, final I18n i18n, final StageManager stageManager) {
		this.i18n = i18n;
		this.stageManager = stageManager;

		this.machineExtensionPatterns = FactoryProvider.EXTENSION_TO_FACTORY_MAP.keySet()
			.stream()
			.map(ext -> EXTENSION_PATTERN_PREFIX + ext)
			.collect(Collectors.toList());
		this.machineExtensionFilters = new ArrayList<>();
		FactoryProvider.FACTORY_TO_EXTENSIONS_MAP.forEach((factory, extensions) -> {
			final String name;
			if (FACTORY_TO_TYPE_KEY_MAP.containsKey(factory)) {
				name = i18n.translate(FACTORY_TO_TYPE_KEY_MAP.get(factory));
			} else {
				name = factory.getSimpleName();
			}
			this.machineExtensionFilters.add(getExtensionFilterUnlocalized(name, extensions));
		});

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
	
	public boolean checkIfPathAlreadyContainsFiles(Path path, String prefix, String contentBundleKey) throws IOException {
		try (final Stream<Path> children = Files.list(path)) {
			if (children.anyMatch(p -> p.getFileName().toString().startsWith(prefix))) {
				// Directory already contains test case trace - ask if the user really wants to save here.
				final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", contentBundleKey, path).showAndWait();
				if (selected.isEmpty() || selected.get() != ButtonType.YES) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * <p>Create a {@link FileChooser.ExtensionFilter} with the list of extensions automatically appended to the description.</p>
	 * <p>Note: The extension strings passed into this method should be plain extensions and not patterns, unlike with the {@link FileChooser.ExtensionFilter#ExtensionFilter(String, List)} constructor. The extension strings should <i>not</i> include a {@code *.} prefix (it is added automatically by this method).</p>
	 * 
	 * @param description the description for this filter
	 * @param extensions the list of extensions for this filter, each as a plain extension without a {@code *.} prefix
	 * @return an extension filter with the given settings
	 */
	public static FileChooser.ExtensionFilter getExtensionFilterUnlocalized(final String description, final List<String> extensions) {
		final List<String> extensionPatterns = extensions.stream()
			.map(ext -> {
				if (ext.startsWith(EXTENSION_PATTERN_PREFIX)) {
					throw new IllegalArgumentException(String.format(Locale.ROOT, "Extensions passed to getExtensionFilter must not include a pattern (%s) prefix: %s", EXTENSION_PATTERN_PREFIX, ext));
				}
				return EXTENSION_PATTERN_PREFIX + ext;
			})
			.collect(Collectors.toList());
		final String descriptionWithPatterns = String.format("%s (%s)",
			description,
			String.join(", ", extensionPatterns)
		);
		return new FileChooser.ExtensionFilter(descriptionWithPatterns, extensionPatterns);
	}
	
	/**
	 * Convenience method, equivalent to {@link #getExtensionFilterUnlocalized(String, List)}, but accepts a bundle key for the description.
	 * 
	 * @param descriptionBundleKey bundle key for the description for this filter
	 * @param extensions the list of extensions for this filter, each as a plain extension without a {@code *.} prefix
	 * @return an extension filter with the given settings
	 */
	public FileChooser.ExtensionFilter getExtensionFilter(final String descriptionBundleKey, final List<String> extensions) {
		return getExtensionFilterUnlocalized(i18n.translate(descriptionBundleKey), extensions);
	}
	
	/**
	 * Convenience method, equivalent to {@link #getExtensionFilter(String, List)}, but accepts an array or varargs.
	 * 
	 * @param descriptionBundleKey bundle key for the description for this filter
	 * @param extensions the list of extensions for this filter, each as a plain extension without a {@code *.} prefix
	 * @return an extension filter with the given settings
	 */
	public FileChooser.ExtensionFilter getExtensionFilter(final String descriptionBundleKey, final String... extensions) {
		return this.getExtensionFilter(descriptionBundleKey, Arrays.asList(extensions));
	}
	
	/**
	 * Create a {@link FileChooser.ExtensionFilter} that matches all file types.
	 * 
	 * @return an extension filter that matches all file types
	 */
	public FileChooser.ExtensionFilter getAllExtensionsFilter() {
		return new FileChooser.ExtensionFilter(i18n.translate("common.fileChooser.fileTypes.all"), "*.*");
	}
	
	public FileChooser.ExtensionFilter getProB2ProjectFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.proB2Project", ProjectManager.PROJECT_FILE_EXTENSION);
	}

	public FileChooser.ExtensionFilter getProB2TraceFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION);
	}

	public FileChooser.ExtensionFilter getSimBFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.simulation", SimulationFileHandler.SIMULATION_FILE_EXTENSION);
	}
	
	public FileChooser.ExtensionFilter getPlainTextFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.text", "txt");
	}
	
	public FileChooser.ExtensionFilter getCsvFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.csv", "csv");
	}
	
	public FileChooser.ExtensionFilter getSvgFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.svg", "svg");
	}

	public FileChooser.ExtensionFilter getPngFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.png", "png");
	}

	public FileChooser.ExtensionFilter getPdfFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.pdf", "pdf");
	}

	public FileChooser.ExtensionFilter getDotFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.dot", "dot");
	}

	public FileChooser.ExtensionFilter getPumlFilter() {
		return this.getExtensionFilter("common.fileChooser.fileTypes.puml", "puml");
	}

	public Path showOpenFileChooser(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}

		final File file = fileChooser.showOpenDialog(window);
		if (file == null) {
			// User canceled the open dialog.
			return null;
		}

		Path path = file.toPath();
		setInitialDirectory(kind, path.getParent());
		return path;
	}

	public Path showSaveFileChooser(final FileChooser fileChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			fileChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}

		final File file = fileChooser.showSaveDialog(window);
		if (file == null) {
			// User canceled the save dialog.
			return null;
		}

		Path path = file.toPath();
		if (Kind.NEW_MACHINE.equals(kind)) {
			String ext = MoreFiles.getFileExtension(path);
			String expectedExtFile = "*." + ext;
			if (ext.isEmpty() || fileChooser.getExtensionFilters().stream().noneMatch(extensionFilter -> extensionFilter.getExtensions().contains(expectedExtFile))) {
				// Either there is no file extension or an invalid one
				stageManager.makeAlert(
						Alert.AlertType.WARNING,
						"common.fileChooser.invalidExtension.warning.header",
						"common.fileChooser.invalidExtension.warning.content",
						ext.isEmpty() ? i18n.translate("common.fileChooser.invalidExtension.warning.content.empty") : ext
				).showAndWait();
				fileChooser.setTitle(i18n.translate("common.fileChooser.invalidExtension.fileChooser.header"));
				return showSaveFileChooser(fileChooser, kind, window);
			}
		}

		setInitialDirectory(kind, path.getParent());

		// this is a hack to fix files with a double extension
		// https://bugs.openjdk.org/browse/JDK-8352298
		if (Files.isRegularFile(path) && path.getFileName() != null) {
			String fileName = path.getFileName().toString();
			String ext = MoreFiles.getFileExtension(path);
			if (!ext.isEmpty() && fileName.endsWith("." + ext + "." + ext)) {
				path = path.resolveSibling(fileName.substring(0, fileName.length() - ext.length() - 1));
			}
		}

		return path;
	}

	public Path showDirectoryChooser(final DirectoryChooser directoryChooser, final Kind kind, final Window window) {
		if (containsValidInitialDirectory(kind)) {
			directoryChooser.setInitialDirectory(getInitialDirectory(kind).toFile());
		}

		final File dirFile = directoryChooser.showDialog(window);
		if (dirFile == null) {
			// User canceled the open dialog.
			return null;
		}

		Path dirPath = dirFile.toPath();
		setInitialDirectory(kind, dirPath.getParent());
		return dirPath;
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB file.
	 * 
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @param projects whether projects should be selectable
	 * @param machines whether machines should be selectable
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	private Path showOpenProjectOrMachineChooser(final Window window, final boolean projects, final boolean machines, final boolean traces, final boolean visualisations) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("common.fileChooser.open.title"));
		
		final List<String> allExtensionPatterns = new ArrayList<>();
		if (projects) {
			allExtensionPatterns.add(EXTENSION_PATTERN_PREFIX + ProjectManager.PROJECT_FILE_EXTENSION);
			fileChooser.getExtensionFilters().add(this.getProB2ProjectFilter());
		}
		
		if (machines) {
			allExtensionPatterns.addAll(this.machineExtensionPatterns);
			fileChooser.getExtensionFilters().addAll(this.machineExtensionFilters);
		}
		if (traces) {
			allExtensionPatterns.add(EXTENSION_PATTERN_PREFIX + TraceFileHandler.TRACE_FILE_EXTENSION);
			fileChooser.getExtensionFilters().addAll(this.getProB2TraceFilter());
		}
		if(visualisations) {
			allExtensionPatterns.add(EXTENSION_PATTERN_PREFIX + "json");
			fileChooser.getExtensionFilters().addAll(this.getExtensionFilter("common.fileChooser.fileTypes.simOrVisB", "json"));
		}

		// This extension filter is created manually instead of with getExtensionFilter,
		// so that the list of extensions doesn't get appended (it would be very long).
		fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(i18n.translate("common.fileChooser.fileTypes.allProB"), allExtensionPatterns));
		return this.showOpenFileChooser(fileChooser, FileChooserManager.Kind.PROJECTS_AND_MACHINES, window);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB 2 project.
	 * 
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenProjectChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, true, false, false, false);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenMachineChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, false, true, false, false);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to select a ProB 2 project or a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showOpenProjectOrMachineChooser(final Window window) {
		return showOpenProjectOrMachineChooser(window, true, true, false, false);
	}

	public Path showOpenAnyFileChooser(final Window window){
		return showOpenProjectOrMachineChooser(window, true, true, true, true);
	}
	
	/**
	 * Show a {@link FileChooser} to ask the user to save a machine file.
	 *
	 * @param window the {@link Window} on which to show the {@link FileChooser}
	 * @return the selected {@link Path}, or {@code null} if none was selected
	 */
	public Path showSaveMachineChooser(final Window window) {
		// remove all unsupported ProB file types:
		// Classical B: ref, imp, sys, def;
		// Event-B: all (eventb, bum, buc);
		// Z: all (zed, tex);
		// Fuzz: all (fuzz).
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));

		List<String> supportedNewMachineExtensionPatterns = new ArrayList<>(this.machineExtensionPatterns);
		String[] notSupported = {"*.ref", "*.imp", "*.sys", "*.def", "*.bum", "*.buc", "*.eventb", "*.zed", "*.tex", "*.fuzz"};
		supportedNewMachineExtensionPatterns.removeAll(Arrays.asList(notSupported));

		List<FileChooser.ExtensionFilter> supportedNewMachineExtenionFilters = new ArrayList<>();
		FactoryProvider.FACTORY_TO_EXTENSIONS_MAP.forEach((factory, extensions) -> {
			final String name;
			if (NEW_MACHINE_FACTORY_TO_TYPE_KEY_MAP.containsKey(factory)) {
				name = i18n.translate(NEW_MACHINE_FACTORY_TO_TYPE_KEY_MAP.get(factory));
			} else {
				return;
			}
			if (factory.equals(ClassicalBFactory.class)) {
				supportedNewMachineExtenionFilters.add(getExtensionFilterUnlocalized(name, Collections.singletonList("mch")));
			} else {
				supportedNewMachineExtenionFilters.add(getExtensionFilterUnlocalized(name, extensions));
			}
		});

		fileChooser.getExtensionFilters().addAll(supportedNewMachineExtenionFilters);
		// This extension filter is created manually instead of with getExtensionFilter,
		// so that the list of extensions doesn't get appended (it would be very long).
		fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter(i18n.translate("common.fileChooser.fileTypes.allSupportedProB"), supportedNewMachineExtensionPatterns));
		return this.showSaveFileChooser(fileChooser, FileChooserManager.Kind.NEW_MACHINE, window);
	}

	private boolean containsValidInitialDirectory(Kind kind) {
		return kind != null && initialDirectories.containsKey(kind) && Files.isDirectory(initialDirectories.get(kind));
	}

	private Path getInitialDirectory(Kind kind) {
		return this.initialDirectories.get(kind);
	}

	private void setInitialDirectory(Kind kind, Path dir) {
		if (kind != null && dir != null && Files.isDirectory(dir)) {
			initialDirectories.put(kind, dir);
		}
	}

	private Map<Kind, Path> getInitialDirectories() {
		return initialDirectories;
	}
}
