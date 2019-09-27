package de.prob2.ui.menu;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetInternalRepresentationPrettyPrintCommand;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.ProjectManager;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

@FXMLInjected
public class FileMenu extends Menu {
	@FXML
	private MenuItem preferencesItem;
	@FXML
	private Menu recentProjectsMenu;
	@FXML
	private MenuItem recentProjectsPlaceholder;
	@FXML
	private MenuItem clearRecentProjects;
	@FXML
	private MenuItem saveMachineItem;
	@FXML
	private MenuItem saveProjectItem;
	@FXML
	private MenuItem viewFormattedCodeItem;
	@FXML
	private MenuItem reloadMachineItem;


	private final RecentProjects recentProjects;
	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final BEditorView bEditorView;
	private final Injector injector;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;

	@Inject
	private FileMenu(
		final RecentProjects recentProjects,
		final ProjectManager projectManager,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace,
		final BEditorView bEditorView,
		final Injector injector,
		final StageManager stageManager,
		final FileChooserManager fileChooserManager
	) {
		this.recentProjects = recentProjects;
		this.projectManager = projectManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.bEditorView = bEditorView;
		this.injector = injector;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "fileMenu.fxml");
	}

	@FXML
	public void initialize() {
		final ListChangeListener<Path> recentProjectsListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentProjectsMenu.getItems();
			final List<MenuItem> newItems = getRecentProjectItems(recentProjects);
			this.clearRecentProjects.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentProjectsPlaceholder);
			}else {
				newItems.get(0).setAccelerator(KeyCombination.valueOf("Shift+Shortcut+'O'"));
			}
			newItems.addAll(recentItems.subList(recentItems.size() - 2, recentItems.size()));
			this.recentProjectsMenu.getItems().setAll(newItems);
		};
		this.recentProjects.addListener(recentProjectsListener);
		recentProjectsListener.onChanged(null);

		this.saveMachineItem.disableProperty().bind(bEditorView.pathProperty().isNull().or(bEditorView.savedProperty()));
		this.saveProjectItem.disableProperty().bind(currentProject.existsProperty().not());
		
		this.viewFormattedCodeItem.disableProperty().bind(currentTrace.existsProperty().not());
		MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
		this.reloadMachineItem.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()));
	}

	@FXML
	public void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	public void handleOpen() {
		final Path selected = fileChooserManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}
		final String ext = Files.getFileExtension(selected.getFileName().toString());
		if ("prob2project".equals(ext)) {
			projectManager.openProject(selected);
		} else {
			projectManager.openAutomaticProjectFromMachine(selected);
		}
	}
	
	@FXML
	private void handleOpenLastProject() {
		if(this.recentProjects.isEmpty()) {
			return;
		}
		projectManager.openProject(this.recentProjects.get(0));
	}

	@FXML
	private void handleClearRecentProjects() {
		this.recentProjects.clear();
	}

	@FXML
	private void saveMachine() {
		this.bEditorView.handleSave();
	}

	@FXML
	private void saveProject() {
		projectManager.saveCurrentProject();
	}

	@FXML
	public void handleViewFormattedCode() {
		final GetInternalRepresentationPrettyPrintCommand cmd = new GetInternalRepresentationPrettyPrintCommand();
		this.currentTrace.getStateSpace().execute(cmd);
		final ViewCodeStage stage = injector.getInstance(ViewCodeStage.class);
		stage.setTitle(currentProject.getCurrentMachine().getName());
		stage.setCode(cmd.getPrettyPrint());
		stage.show();
	}

	@FXML
	private void handleReloadMachine() {
		currentProject.reloadCurrentMachine();
	}

	private List<MenuItem> getRecentProjectItems(final List<Path> recentList) {
		final List<MenuItem> newItems = new ArrayList<>();
		for (final Path path : recentList) {
			final MenuItem item = new MenuItem(path.getFileName().toString());
			item.setOnAction(event -> projectManager.openProject(path));
			newItems.add(item);
		}
		return newItems;
	}

	MenuItem getPreferencesItem() {
		return preferencesItem;
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	public Menu getRecentProjectsMenu() {
		return recentProjectsMenu;
	}
}
