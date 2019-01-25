package de.prob2.ui.statusbar;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

@FXMLInjected
@Singleton
public class StatusBar extends HBox {
	public enum LoadingStatus {
		NOT_LOADING("common.noModelLoaded"),
		LOADING_FILE("statusbar.loadStatus.loadingFile"),
		ADDING_ANIMATION("statusbar.loadStatus.addingAnimation"),
		;
		
		private final String messageKey;
		
		LoadingStatus(final String messageKey) {
			this.messageKey = messageKey;
		}
		
		public String getMessageKey() {
			return this.messageKey;
		}
	}
	
	public enum CheckingStatus {
		ERROR, SUCCESSFUL
	}
	
	@FXML private Label statusLabel;
	
	private final ResourceBundle resourceBundle;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	
	private final ObjectProperty<StatusBar.LoadingStatus> loadingStatus;
	private final ObjectProperty<StatusBar.CheckingStatus> ltlStatus;
	private final ObjectProperty<StatusBar.CheckingStatus> symbolicStatus;
	private final ObjectProperty<StatusBar.CheckingStatus> modelcheckingStatus;
	private final BooleanProperty operationsViewUpdating;
	private final BooleanProperty statesViewUpdating;
	
	@Inject
	private StatusBar(final ResourceBundle resourceBundle, final CurrentTrace currentTrace, final CurrentProject currentProject,
					final StageManager stageManager) {
		super();
		
		this.resourceBundle = resourceBundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", StatusBar.LoadingStatus.NOT_LOADING);
		this.ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.symbolicStatus = new SimpleObjectProperty<>(this, "symbolicStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.operationsViewUpdating = new SimpleBooleanProperty(this, "operationsViewUpdating", false);
		this.statesViewUpdating = new SimpleBooleanProperty(this, "statesViewUpdating", false);
		
		stageManager.loadFXML(this, "status_bar.fxml");
	}
	
	@FXML
	private void initialize() {
		this.currentTrace.addListener((observable, from, to) -> this.update());
		this.currentProject.addListener((observable, from, to) -> {
			reset();
			this.update();
		});
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> reset());
		this.loadingStatusProperty().addListener((observable, from, to) -> this.update());
		this.ltlStatusProperty().addListener((observable, from, to) -> this.update());
		this.symbolicStatusProperty().addListener((observable, from, to) -> this.update());
		this.modelcheckingStatusProperty().addListener((observable, from, to) -> this.update());
		this.operationsViewUpdatingProperty().addListener((o, from, to) -> this.update());
		this.statesViewUpdatingProperty().addListener((o, from, to) -> this.update());
	}
	
	public ObjectProperty<StatusBar.LoadingStatus> loadingStatusProperty() {
		return this.loadingStatus;
	}
	
	public StatusBar.LoadingStatus getLoadingStatus() {
		return this.loadingStatusProperty().get();
	}
	
	public void setLoadingStatus(final StatusBar.LoadingStatus loadingStatus) {
		this.loadingStatusProperty().set(loadingStatus);
	}
	
	public ObjectProperty<StatusBar.CheckingStatus> ltlStatusProperty() {
		return this.ltlStatus;
	}
	
	public StatusBar.CheckingStatus getLtlStatus() {
		return this.ltlStatusProperty().get();
	}
		
	public void setLtlStatus(final StatusBar.CheckingStatus ltlStatus) {
		this.ltlStatusProperty().set(ltlStatus);
	}
	
	public ObjectProperty<StatusBar.CheckingStatus> symbolicStatusProperty() {
		return this.symbolicStatus;
	}
	
	public StatusBar.CheckingStatus getSymbolicStatus() {
		return this.symbolicStatusProperty().get();
	}
	
	public void setSymbolicStatus(final StatusBar.CheckingStatus symbolicStatus) {
		this.symbolicStatusProperty().set(symbolicStatus);
	}
	
	public ObjectProperty<StatusBar.CheckingStatus> modelcheckingStatusProperty() {
		return this.modelcheckingStatus;
	}
	
	public StatusBar.CheckingStatus getModelcheckingStatus() {
		return this.modelcheckingStatusProperty().get();
	}
	
	public void setModelcheckingStatus(final StatusBar.CheckingStatus modelcheckingStatus) {
		this.modelcheckingStatus.set(modelcheckingStatus);
	}
	
	public BooleanProperty operationsViewUpdatingProperty() {
		return this.operationsViewUpdating;
	}
	
	public boolean isOperationsViewUpdating() {
		return this.operationsViewUpdatingProperty().get();
	}
	
	public void setOperationsViewUpdating(final boolean operationsViewUpdating) {
		this.operationsViewUpdatingProperty().set(operationsViewUpdating);
	}
	
	public BooleanProperty statesViewUpdatingProperty() {
		return this.statesViewUpdating;
	}
	
	public boolean isStatesViewUpdating() {
		return this.statesViewUpdatingProperty().get();
	}
	
	public void setStatesViewUpdating(final boolean statesViewUpdating) {
		this.statesViewUpdatingProperty().set(statesViewUpdating);
	}
	
	private void update() {
		statusLabel.getStyleClass().removeAll("noErrors", "someErrors");
		if (this.isOperationsViewUpdating() || this.isStatesViewUpdating()) {
			statusLabel.setText(resourceBundle.getString("statusbar.updatingViews"));
		} else {
			final Trace trace = this.currentTrace.get();
			if (trace != null) {
				final List<String> errorMessages = getErrorMessages(trace);
				if (errorMessages.isEmpty()) {
					statusLabel.getStyleClass().add("noErrors");
					statusLabel.setText(resourceBundle.getString("statusbar.noErrors"));
				} else {
					statusLabel.getStyleClass().add("someErrors");
					statusLabel.setText(String.format(resourceBundle.getString("statusbar.someErrors"), String.join(", ", errorMessages)));
				}
			} else {
				statusLabel.setText(this.resourceBundle.getString(this.getLoadingStatus().getMessageKey()));
			}
		}
	}

	private List<String> getErrorMessages(final Trace trace) {
		final List<String> errorMessages = new ArrayList<>();
		if (!trace.getCurrentState().isInvariantOk()) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.invariantNotOK"));
		}
		if (!trace.getCurrentState().getStateErrors().isEmpty()) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.stateErrors"));
		}
		if (this.getLtlStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.ltlError"));
		}

		if (this.getSymbolicStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.symbolicError"));
		}

		if (this.getModelcheckingStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.modelcheckError"));
		}
		return errorMessages;
	}

	public void reset() {
		setModelcheckingStatus(CheckingStatus.SUCCESSFUL);
		setLtlStatus(CheckingStatus.SUCCESSFUL);
		setSymbolicStatus(CheckingStatus.SUCCESSFUL);
		setLoadingStatus(LoadingStatus.NOT_LOADING);
		setOperationsViewUpdating(false);
		setStatesViewUpdating(false);
	}
	
}
