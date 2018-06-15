package de.prob2.ui.menu;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetInternalRepresentationPrettyPrintCommand;

import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.modelchecking.ModelcheckingView;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final BEditorView bEditorView;
	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	private FileMenu(final StageManager stageManager, final RecentProjects recentProjects,
			final CurrentProject currentProject, final CurrentTrace currentTrace, final BEditorView bEditorView, final Injector injector, final ResourceBundle bundle) {
		this.recentProjects = recentProjects;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.bEditorView = bEditorView;
		this.injector = injector;
		this.stageManager = stageManager;
		this.bundle = bundle;
		stageManager.loadFXML(this, "fileMenu.fxml");
	}

	@FXML
	public void initialize() {
		final ListChangeListener<String> recentProjectsListener = change -> {
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
		this.reloadMachineItem.disableProperty().bind(currentTrace.existsProperty().not().or(currentProject.currentMachineProperty().isNull()));
	}

	@FXML
	public void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	public void handleOpen() {
		final File selected = stageManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}
		final Path path = selected.toPath();
		final String ext = StageManager.getExtension(path.getFileName().toString());
		if ("pb2project".equals(ext)) {
			this.openProject(path);
		} else {
			this.createProjectFromFile(path);
		}
	}
	
	@FXML
	private void handleOpenLastProject() {
		if(this.recentProjects.isEmpty()) {
			return;
		}
		this.openProject(Paths.get(this.recentProjects.get(0)));
	}

	private void createProjectFromFile(Path path) {
		final Path projectLocation = path.getParent();
		final Path relative = projectLocation.relativize(path);
		final String fileName = path.getFileName().toString();
		final String shortName = fileName.substring(0, fileName.lastIndexOf('.'));
		final String description = String.format(bundle.getString("project.automaticDescription"), path);
		final Machine machine = new Machine(shortName, "", relative);
		currentProject.set(new Project(shortName, description, machine, projectLocation), true);

		currentProject.startAnimation(machine, Preference.DEFAULT);
	}

	private void openProject(Path path) {
		injector.getInstance(ProjectManager.class).openProject(path);

		Platform.runLater(() -> injector.getInstance(ModelcheckingView.class).resetView());
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
		injector.getInstance(ProjectManager.class).saveCurrentProject();
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

	@FXML
	private void handleClose() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}

	private List<MenuItem> getRecentProjectItems(SimpleListProperty<String> recentListProperty) {
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : recentListProperty) {
			Path path = Paths.get(s);
			final MenuItem item = new MenuItem(path.getFileName().toString());
			item.setOnAction(event -> this.openProject(path));
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
