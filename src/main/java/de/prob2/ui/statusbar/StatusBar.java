package de.prob2.ui.statusbar;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.Translatable;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentTrace;

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
public final class StatusBar extends HBox {
	public enum LoadingStatus implements Translatable {
		NOT_LOADING("common.noModelLoaded"),
		PARSING_FILE("statusbar.loadStatus.parsingFile"),
		STARTING_ANIMATOR("statusbar.loadStatus.startingAnimator"),
		LOADING_MODEL("statusbar.loadStatus.loadingModel"),
		SETTING_CURRENT_MODEL("statusbar.loadStatus.settingCurrentModel"),
		;
		
		private final String messageKey;
		
		LoadingStatus(final String messageKey) {
			this.messageKey = messageKey;
		}

		@Override
		public String getTranslationKey() {
			return this.messageKey;
		}
	}
	
	@FXML private Label statusLabel;
	@FXML private BindableGlyph infoIcon;
	
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final Provider<ErrorStatusStage> errorStatusStageProvider;
	
	private final ObjectProperty<StatusBar.LoadingStatus> loadingStatus;
	private BooleanExpression updating;
	
	@Inject
	private StatusBar(final I18n i18n, final CurrentTrace currentTrace,
			final Provider<ErrorStatusStage> errorStatusStageProvider, final StageManager stageManager) {
		super();
		
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.errorStatusStageProvider = errorStatusStageProvider;
		this.loadingStatus = new SimpleObjectProperty<>(this, "loadingStatus", StatusBar.LoadingStatus.NOT_LOADING);
		this.updating = Bindings.createBooleanBinding(() -> false);
		
		stageManager.loadFXML(this, "status_bar.fxml");
	}
	
	@FXML
	private void initialize() {
		final InvalidationListener updateListener = o -> this.update();
		this.currentTrace.addListener(updateListener);
		this.loadingStatusProperty().addListener(updateListener);
		// this.updating doesn't have a listener; instead each individual expression has a listener added in addUpdatingExpression.
	}
	
	@FXML
	private void showErrorStatusStage() {
		this.errorStatusStageProvider.get().show();
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
		statusLabel.getStyleClass().removeAll("no-error", "warning", "error");
		infoIcon.setVisible(false);
		if (this.updating.get()) {
			statusLabel.setText(i18n.translate("statusbar.updatingViews"));
		} else {
			final Trace trace = this.currentTrace.get();
			if (trace != null) {
				final List<String> errorMessages = getErrorMessages(trace);
				if (errorMessages.isEmpty()) {
					final List<String> warningMessages = getWarningMessages(trace);
					if (warningMessages.isEmpty()) {
						statusLabel.getStyleClass().add("no-error");
						statusLabel.setText(i18n.translate("statusbar.noErrors"));
					} else {
						statusLabel.getStyleClass().add("warning");
						statusLabel.setText(i18n.translate("statusbar.warnings", String.join(", ", warningMessages)));
					}
				} else {
					statusLabel.getStyleClass().add("error");
					statusLabel.setText(i18n.translate("statusbar.someErrors", String.join(", ", errorMessages)));
				}
				infoIcon.setVisible(true);
			} else {
				statusLabel.setText(i18n.translate(this.getLoadingStatus()));
			}
		}
	}

	private List<String> getErrorMessages(final Trace trace) {
		final List<String> errorMessages = new ArrayList<>();
		if (!trace.getCurrentState().isInvariantOk()) {
			errorMessages.add(i18n.translate("statusbar.errors.invariantNotOK"));
		}
		if (!trace.getCurrentState().getStateErrors().isEmpty()) {
			errorMessages.add(i18n.translate("statusbar.errors.stateErrors"));
		}
		return errorMessages;
	}

	private List<String> getWarningMessages(final Trace trace) {
		final List<String> warningMessages = new ArrayList<>();
		if (trace.getCurrentState().getOutTransitions().isEmpty()) {
			warningMessages.add(i18n.translate("statusbar.warnings.deadlock"));
		}
		return warningMessages;
	}
}
