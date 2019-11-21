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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

@FXMLInjected
@Singleton
public class StatusBar extends HBox {
	public enum LoadingStatus {
		NOT_LOADING("common.noModelLoaded"),
		PARSING_FILE("statusbar.loadStatus.parsingFile"),
		LOADING_MODEL("statusbar.loadStatus.loadingModel"),
		SETTING_CURRENT_MODEL("statusbar.loadStatus.settingCurrentModel"),
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
	private final ObjectProperty<StatusBar.CheckingStatus> symbolicCheckingStatus;
	private final ObjectProperty<StatusBar.CheckingStatus> symbolicAnimationStatus;
	private final ObjectProperty<StatusBar.CheckingStatus> modelcheckingStatus;
	private BooleanExpression updating;
	
	@Inject
	private StatusBar(final ResourceBundle resourceBundle, final CurrentTrace currentTrace, final CurrentProject currentProject,
					final StageManager stageManager) {
		super();
		
		this.resourceBundle = resourceBundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", StatusBar.LoadingStatus.NOT_LOADING);
		this.ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.symbolicCheckingStatus = new SimpleObjectProperty<>(this, "symbolicCheckingStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.symbolicAnimationStatus = new SimpleObjectProperty<>(this, "symbolicAnimationStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.modelcheckingStatus = new SimpleObjectProperty<>(this, "modelcheckingStatus", StatusBar.CheckingStatus.SUCCESSFUL);
		this.updating = Bindings.createBooleanBinding(() -> false);
		
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
		this.symbolicCheckingStatusProperty().addListener((observable, from, to) -> this.update());
		this.symbolicAnimationStatusProperty().addListener((observable, from, to) -> this.update());
		this.modelcheckingStatusProperty().addListener((observable, from, to) -> this.update());
		// this.updating doesn't have a listener; instead each individual expression has a listener added in addUpdatingExpression.
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

	public ObjectProperty<StatusBar.CheckingStatus> symbolicCheckingStatusProperty() {
		return this.symbolicCheckingStatus;
	}

	public StatusBar.CheckingStatus getSymbolicCheckingStatus() {
		return this.symbolicCheckingStatusProperty().get();
	}

	public void setSymbolicCheckingStatus(final StatusBar.CheckingStatus symbolicCheckingStatus) {
		this.symbolicCheckingStatusProperty().set(symbolicCheckingStatus);
	}

	public ObjectProperty<StatusBar.CheckingStatus> symbolicAnimationStatusProperty() {
		return this.symbolicAnimationStatus;
	}

	public StatusBar.CheckingStatus getSymbolicAnimationStatus() {
		return this.symbolicAnimationStatusProperty().get();
	}

	public void setSymbolicAnimationStatus(final StatusBar.CheckingStatus symbolicAnimationStatus) {
		this.symbolicAnimationStatusProperty().set(symbolicAnimationStatus);
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
	
	public void addUpdatingExpression(final BooleanExpression expr) {
		this.updating = this.updating.or(expr);
		expr.addListener(o -> Platform.runLater(this::update));
		Platform.runLater(this::update);
	}
	
	private void update() {
		statusLabel.getStyleClass().removeAll("noErrors", "someErrors");
		if (this.updating.get()) {
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

		if (this.getSymbolicCheckingStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.symbolic.checking.error"));
		}

		if (this.getSymbolicAnimationStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.symbolic.animation.error"));
		}

		if (this.getModelcheckingStatus() == StatusBar.CheckingStatus.ERROR) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.modelcheckError"));
		}
		return errorMessages;
	}

	public void reset() {
		setModelcheckingStatus(CheckingStatus.SUCCESSFUL);
		setLtlStatus(CheckingStatus.SUCCESSFUL);
		setSymbolicCheckingStatus(CheckingStatus.SUCCESSFUL);
		setSymbolicAnimationStatus(CheckingStatus.SUCCESSFUL);
		setLoadingStatus(LoadingStatus.NOT_LOADING);
	}
	
}
