package de.prob2.ui.statusbar;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

@Singleton
public class StatusBar extends HBox {
	public enum LoadingStatus {
		NOT_LOADING("common.noModelLoaded"),
		LOADING_FILE("statusbar.loadStatus.loadingFile"),
		REMOVING_OLD_ANIMATION("statusbar.loadStatus.removingOldAnimation"),
		ADDING_ANIMATION("statusbar.loadStatus.addingAnimation"),
		;
		
		private final String messageKey;
		
		private LoadingStatus(final String messageKey) {
			this.messageKey = messageKey;
		}
		
		public String getMessageKey() {
			return this.messageKey;
		}
	}
	
	public enum LTLStatus {
		ERROR, SUCCESSFUL;
	}
	
	public enum CBCStatus {
		ERROR, SUCCESSFUL;
	}
	
	@FXML private Label statusLabel;
	
	private final ResourceBundle resourceBundle;
	private final CurrentTrace currentTrace;
	
	private final ObjectProperty<StatusBar.LoadingStatus> loadingStatus;
	private final ObjectProperty<StatusBar.LTLStatus> ltlStatus;
	private final ObjectProperty<StatusBar.CBCStatus> cbcStatus;
	private final BooleanProperty operationsViewUpdating;
	private final BooleanProperty statesViewUpdating;
	
	@Inject
	private StatusBar(final ResourceBundle resourceBundle, final CurrentTrace currentTrace, final StageManager stageManager) {
		super();
		
		this.resourceBundle = resourceBundle;
		this.currentTrace = currentTrace;
		
		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", StatusBar.LoadingStatus.NOT_LOADING);
		this.ltlStatus = new SimpleObjectProperty<>(this, "ltlStatus", StatusBar.LTLStatus.SUCCESSFUL);
		this.cbcStatus = new SimpleObjectProperty<>(this, "cbcStatus", StatusBar.CBCStatus.SUCCESSFUL);
		this.operationsViewUpdating = new SimpleBooleanProperty(this, "operationsViewUpdating", false);
		this.statesViewUpdating = new SimpleBooleanProperty(this, "statesViewUpdating", false);
		
		stageManager.loadFXML(this, "status_bar.fxml");
	}
	
	@FXML
	private void initialize() {
		this.currentTrace.addListener((observable, from, to) -> this.update());
		this.loadingStatusProperty().addListener((observable, from, to) -> this.update());
		this.ltlStatusProperty().addListener((observable, from, to) -> this.update());
		this.cbcStatusProperty().addListener((observable, from, to) -> this.update());
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
	
	public ObjectProperty<StatusBar.LTLStatus> ltlStatusProperty() {
		return this.ltlStatus;
	}
	
	public StatusBar.LTLStatus getLtlStatus() {
		return this.ltlStatusProperty().get();
	}
		
	public void setLtlStatus(final StatusBar.LTLStatus ltlStatus) {
		this.ltlStatusProperty().set(ltlStatus);
	}
	
	public ObjectProperty<StatusBar.CBCStatus> cbcStatusProperty() {
		return this.cbcStatus;
	}
	
	public StatusBar.CBCStatus getCbcStatus() {
		return this.cbcStatusProperty().get();
	}
	
	public void setCbcStatus(final StatusBar.CBCStatus cbcStatus) {
		this.cbcStatusProperty().set(cbcStatus);
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
		} else if (this.currentTrace.exists()) {
			final List<String> errorMessages = new ArrayList<>();
			if (!this.currentTrace.getCurrentState().isInvariantOk()) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.invariantNotOK"));
			}
			if (!this.currentTrace.getCurrentState().getStateErrors().isEmpty()) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.stateErrors"));
			}
			if (this.getLtlStatus() == StatusBar.LTLStatus.ERROR) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.ltlError"));
			}
			
			if (this.getCbcStatus() == StatusBar.CBCStatus.ERROR) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.cbcError"));
			}
			
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
