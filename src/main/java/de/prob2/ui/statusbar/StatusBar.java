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
import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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
		STARTING_PROB_CORE("statusbar.loadStatus.startingProBCore"),
		PREPARING_ANIMATOR("statusbar.loadStatus.preparingAnimator"),
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
	
	@FXML private Label statusLabel;
	
	private final ResourceBundle resourceBundle;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	
	private final ObjectProperty<StatusBar.LoadingStatus> loadingStatus;
	private BooleanExpression updating;
	
	@Inject
	private StatusBar(final ResourceBundle resourceBundle, final CurrentTrace currentTrace, final CurrentProject currentProject,
					final StageManager stageManager) {
		super();
		
		this.resourceBundle = resourceBundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", StatusBar.LoadingStatus.NOT_LOADING);
		this.updating = Bindings.createBooleanBinding(() -> false);
		
		stageManager.loadFXML(this, "status_bar.fxml");
	}
	
	@FXML
	private void initialize() {
		final InvalidationListener updateListener = o -> this.update();
		this.currentTrace.addListener(updateListener);
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.modelcheckingStatusProperty().removeListener(updateListener);
				from.ltlStatusProperty().removeListener(updateListener);
				from.symbolicCheckingStatusProperty().removeListener(updateListener);
			}
			if (to != null) {
				to.modelcheckingStatusProperty().addListener(updateListener);
				to.ltlStatusProperty().addListener(updateListener);
				to.symbolicCheckingStatusProperty().addListener(updateListener);
			}
		});
		this.loadingStatusProperty().addListener(updateListener);
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
			final Machine machine = this.currentProject.getCurrentMachine();
			final Trace trace = this.currentTrace.get();
			if (machine != null && trace != null) {
				final List<String> errorMessages = getErrorMessages(machine, trace);
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

	private List<String> getErrorMessages(final Machine machine, final Trace trace) {
		final List<String> errorMessages = new ArrayList<>();
		if (!trace.getCurrentState().isInvariantOk()) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.invariantNotOK"));
		}
		if (!trace.getCurrentState().getStateErrors().isEmpty()) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.stateErrors"));
		}
		if (machine.getLtlStatus() == Machine.CheckingStatus.FAILED) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.ltlError"));
		}

		if (machine.getSymbolicCheckingStatus() == Machine.CheckingStatus.FAILED) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.symbolic.checking.error"));
		}

		if (machine.getModelcheckingStatus() == Machine.CheckingStatus.FAILED) {
			errorMessages.add(resourceBundle.getString("statusbar.errors.modelcheckError"));
		}
		return errorMessages;
	}
}
