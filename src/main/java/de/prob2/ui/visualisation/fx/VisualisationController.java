package de.prob2.ui.visualisation.fx;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.visualisation.fx.listener.EventListener;
import de.prob2.ui.visualisation.fx.listener.FormulaListener;
import de.prob2.ui.visualisation.fx.loader.VisualisationLoader;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @since 14.09.17
 */
@Singleton
public class VisualisationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationController.class);

	private final ChangeListener<Trace> currentTraceChangeListener;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	private final TabPane tabPane;
	private final ReadOnlyObjectProperty<Machine> currentMachine;

	private HashMap<String, List<FormulaListener>> formulaListenerMap;
	private HashMap<String, EventListener> eventListenerMap;
	private SimpleObjectProperty<Visualisation> visualisation = new SimpleObjectProperty<>(null);
	private SimpleBooleanProperty detached = new SimpleBooleanProperty(false);
	private Stage visualizationStage;
	private Tab visualisationTab;
	private AnchorPane placeHolderContent;
	private Label placeHolderLabel;
	private VisualisationModel visualisationModel;
	private VisualisationLoader visualisationLoader;

	@Inject
	public VisualisationController(StageManager stageManager, CurrentTrace currentTrace, CurrentProject currentProject,
								   MainView mainView, ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentMachine = currentProject.currentMachineProperty();
		this.tabPane = mainView.getTabPane();
		this.bundle = bundle;
		visualisationModel = new VisualisationModel(currentTrace, stageManager, bundle);

		currentTraceChangeListener = (observable, oldTrace, newTrace) -> {
			if (newTrace != null) {
				if (newTrace.getCurrentState() != null && newTrace.getCurrentState().isInitialised()) {
					visualisationModel.setTraces(oldTrace, newTrace);
					if (newTrace.getPreviousState() == null || !newTrace.getPreviousState().isInitialised()) {
						//the model was initialized in the last event, so constants could have changed
						setVisualisationContent(visualisation.get().initialize());
					}
					updateVisualization();
				} else {
					setVisualisationContent(getPlaceHolderContent(
							format("visualisation.controller.initialized", currentMachine.get().getName())));
				}
			}
		};

		currentMachine.addListener((observable, oldMachine, newMachine) -> {
			Visualisation visualisation = this.visualisation.get();
			if (visualisation != null) {
				if (newMachine == null) {
					showAlert(Alert.AlertType.INFORMATION,
							format("visualisation.machine.null", oldMachine.getName(), visualisation.getName()),
							ButtonType.OK);
					stopVisualisation();
				} else if (!newMachine.equals(oldMachine)) {
					boolean start = checkMachine(visualisation.getMachines());
					if (start) {
						setVisualisationContent(visualisation.initialize());
					} else {
						showAlert(Alert.AlertType.INFORMATION,
								format("visualisation.machine.loaded", newMachine.getName(), visualisation.getName()),
								ButtonType.OK);
						stopVisualisation();
					}
				}
			}
		});
	}

	public ReadOnlyObjectProperty<Machine> currentMachineProperty() {
		return currentMachine;
	}

	public SimpleObjectProperty<Visualisation> visualisationProperty() {
		return visualisation;
	}

	public SimpleBooleanProperty detachProperty() {return detached;}

	public void openVisualisation() {
		if (visualisation.isNotNull().get()) {
			Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
					format("visualisation.controller.replace", visualisation.get().getName()),
					ButtonType.YES, ButtonType.NO);
			alert.setTitle(bundle.getString("menu.visualisation"));
			alert.initOwner(stageManager.getCurrent());
			Optional<ButtonType> alertResult = alert.showAndWait();
			if (alertResult.isPresent() && alertResult.get() == ButtonType.YES) {
				stopVisualisation();
			} else {
				return;
			}
		}
		LOGGER.debug("Show filechooser to select a visualisation.");
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("visualisation.controller.select"));
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("Visualisation-JAR", "*.jar"),
						new FileChooser.ExtensionFilter("Visualisation-Class", "*.java"));
		File selectedVisualisation = fileChooser.showOpenDialog(stageManager.getCurrent());

		if (selectedVisualisation != null) {
			LOGGER.debug("Try to load visualisation from file {}.", selectedVisualisation.getName());
			if (visualisationLoader == null) {
				visualisationLoader = new VisualisationLoader(stageManager, bundle);
			}
			Visualisation loadedVisualisation = visualisationLoader.loadVisualization(selectedVisualisation);
			if (loadedVisualisation != null) {
				startVisualization(loadedVisualisation);
			} else {
				visualisationLoader.closeClassloader();
			}
		}
	}

	public void registerFormulaListener(FormulaListener listener) {
		String[] formulas = listener.getFormulas();
		if (formulaListenerMap == null) {
			formulaListenerMap = new HashMap<>();
		}
		for(String formula : formulas) {
			if (formulaListenerMap.containsKey(formula)) {
				formulaListenerMap.get(formula).add(listener);
			} else {
				formulaListenerMap.put(formula, new ArrayList<>(Collections.singletonList(listener)));
			}
		}
	}

	public void registerEventListener(EventListener listener) {
		if (eventListenerMap == null) {
			eventListenerMap = new HashMap<>();
		}
		eventListenerMap.put(listener.getEvent(), listener);
	}

	private Node getPlaceHolderContent(String placeHolder) {
		if (placeHolderContent == null) {
			placeHolderContent = new AnchorPane();
			placeHolderLabel = new Label(placeHolder);
			setZeroAnchor(placeHolderLabel);
			placeHolderLabel.setAlignment(Pos.CENTER);
			placeHolderContent.getChildren().add(placeHolderLabel);
		} else {
			placeHolderLabel.setText(placeHolder);
		}
		return placeHolderContent;
	}

	private void startVisualization(Visualisation loadedVisualisation) {
		LOGGER.debug("Starting the visualisation \"{}\"", loadedVisualisation.getName());
		boolean start = checkMachine(loadedVisualisation.getMachines());
		if (!start) {
			showAlert(Alert.AlertType.INFORMATION,
					format("visualisation.controller.unsuitable", loadedVisualisation.getName(), currentMachine.get().getName()),
					ButtonType.OK);
			visualisationLoader.closeClassloader();
			return;
		}
		visualisation.set(loadedVisualisation);
		loadedVisualisation.setController(this);
		loadedVisualisation.setModel(visualisationModel);
		loadedVisualisation.registerFormulaListener();
		loadedVisualisation.registerEventListener();
		createVisualisationTab();
		if (currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised()) {
			LOGGER.debug("Start: The current state is initialised, call initialize() of visualisation.");
			visualisationModel.setTraces(null, currentTrace.get());
			setVisualisationContent(loadedVisualisation.initialize());
			updateVisualization();
		}
		currentTrace.addListener(currentTraceChangeListener);
	}

	public void stopVisualisation() {
		if (visualisation.isNotNull().get()) {
			LOGGER.debug("Stopping visualisation \"{}\"!", visualisation.get().getName());
			currentTrace.removeListener(currentTraceChangeListener);
			if (formulaListenerMap != null && !formulaListenerMap.isEmpty()) {
				formulaListenerMap.clear();
			}
			if (eventListenerMap != null && !eventListenerMap.isEmpty()) {
				eventListenerMap.clear();
			}
			visualisation.get().stop();
			visualisationLoader.closeClassloader();
			visualisation.set(null);
			if (detached.get()) {
				visualizationStage.close();
				detached.set(false);
			}
			closeVisualisationTab();
		}
	}

	private void updateVisualization() {
		//first check which formulas have changed
		LOGGER.debug("Update visualisation!");
		if (formulaListenerMap != null) {
			List<String> changedFormulas = new ArrayList<>();
			for (String formula : formulaListenerMap.keySet()) {
				if (visualisationModel.hasChanged(formula)) {
					changedFormulas.add(formula);
				}
			}

			LOGGER.debug("The following formulas have changed their values: {}", String.join(" ", changedFormulas));

			Set<FormulaListener> listenersToTrigger = new HashSet<>();
			for (String formula : changedFormulas) {
				listenersToTrigger.addAll(formulaListenerMap.get(formula));
			}

			Map<String, Object> formulaValueMap = new HashMap<>(changedFormulas.size());
			for (FormulaListener listener : listenersToTrigger) {
				String[] formulas = listener.getFormulas();
				Object[] formulaValues = new Object[formulas.length];
				for (int i = 0; i < formulas.length; i++) {
					if (formulaValueMap.containsKey(formulas[i])) {
						formulaValues[i] = formulaValueMap.get(formulas[i]);
					} else {
						Object formulaValue = visualisationModel.getValue(formulas[i]);
						formulaValues[i] = formulaValue;
						formulaValueMap.put(formulas[i], formulaValue);
					}
				}
				LOGGER.debug("Call listener for formulas: {}", String.join(" ", formulas));
				try {
					listener.variablesChanged(formulaValues);
				} catch (Exception e) {
					Alert alert = stageManager.makeExceptionAlert(Alert.AlertType.WARNING,
							format("visualisation.controller.listener.exception", String.join(" ", formulas)), e);
					alert.initOwner(stageManager.getCurrent());
					alert.show();
					LOGGER.warn("Exception while calling the formula listener for the formulas:\n\"" +
							String.join(" ", formulas), e);
				}
			}
		}

		if (eventListenerMap != null) {
			String lastEvent = currentTrace.get().getCurrentTransition().getName();
			if (eventListenerMap.containsKey(lastEvent)) {
				LOGGER.info("Last executed event is \"{}\". Call corresponding listener.");
				eventListenerMap.get(lastEvent).eventExcecuted();
			}
		}
	}

	private void createVisualisationTab() {
		visualisationTab = new Tab(visualisation.get().getName(), getPlaceHolderContent(
				format("visualisation.controller.initialized", currentMachine.get().getName())));
		visualisationTab.setClosable(false);
		tabPane.getTabs().add(visualisationTab);
		tabPane.getSelectionModel().select(visualisationTab);
	}

	private void closeVisualisationTab() {
		tabPane.getTabs().remove(visualisationTab);
		visualisationTab = null;
	}

	public void detachVisualisation() {
		final Node visualisationContent = visualisationTab.getContent();
		setZeroAnchor(visualisationContent);
		Scene visualisationScene = new Scene(new AnchorPane(visualisationContent));
		visualizationStage = stageManager.makeStage(visualisationScene, null);
		visualizationStage.setResizable(true);
		visualizationStage.setTitle(visualisation.get().getName());
		visualizationStage.setOnCloseRequest(event -> {
			if (visualisation.isNotNull().get()) {
				visualisationTab.setContent(visualisationContent);
				visualisationTab.getTabPane().getSelectionModel().select(visualisationTab);
			}
			detached.set(false);
			visualizationStage = null;
		});
		visualizationStage.show();
		visualisationTab.setContent(getPlaceHolderContent(bundle.getString("visualisation.controller.detached")));
		detached.set(true);
	}

	private void setVisualisationContent(Node visualisationContent) {
		if (detached.get()) {
			Parent parent  = visualizationStage.getScene().getRoot();
			AnchorPane pane = (parent != null) ? (AnchorPane) parent : new AnchorPane();
			pane.getChildren().clear();
			setZeroAnchor(visualisationContent);
			pane.getChildren().add(visualisationContent);
		} else {
			visualisationTab.setContent(visualisationContent);
		}
	}

	private boolean checkMachine(String[] machines) {
		String machineName = currentMachine.get().getFileName();
		LOGGER.debug("Checking the machine. Current machine is \"{}\" and possible machines are \"{}\"", machineName, machines);
		boolean start = true;
		if (machines != null && machines.length != 0) {
			start = Arrays.asList(machines).contains(machineName);
		}
		return start;
	}

	private void showAlert(Alert.AlertType type, String content, ButtonType... buttons) {
		Alert alert = stageManager.makeAlert(type, content, buttons);
		alert.setTitle(bundle.getString("menu.visualisation"));
		alert.initOwner(stageManager.getCurrent());
		alert.show();
	}

	private String format(String key, Object... args) {
		return String.format(bundle.getString(key), args);
	}

	private void setZeroAnchor(Node node) {
		AnchorPane.setTopAnchor(node, 0.0);
		AnchorPane.setBottomAnchor(node, 0.0);
		AnchorPane.setLeftAnchor(node, 0.0);
		AnchorPane.setRightAnchor(node, 0.0);
	}
}
