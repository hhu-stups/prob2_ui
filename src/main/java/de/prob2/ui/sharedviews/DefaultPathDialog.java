package de.prob2.ui.sharedviews;

import java.nio.file.Path;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public final class DefaultPathDialog extends Dialog<DefaultPathDialog.Action> {
	public enum Action {
		LOAD_DEFAULT,
		SET_CURRENT_AS_DEFAULT,
		UNSET_DEFAULT,
	}
	
	private final I18n i18n;
	
	private String statusWithDefault;
	private String statusWithoutDefault;
	private ButtonType loadButtonType;
	private ButtonType setButtonType;
	private ButtonType unsetButtonType;
	private Path loadedPath;
	private Path defaultPath;
	
	@Inject
	public DefaultPathDialog(final StageManager stageManager, final I18n i18n) {
		super();
		
		this.i18n = i18n;
		
		this.setResultConverter(buttonType -> {
			if (buttonType == null || buttonType == ButtonType.CANCEL) {
				return null;
			} else if (buttonType == this.loadButtonType) {
				return DefaultPathDialog.Action.LOAD_DEFAULT;
			} else if (buttonType == this.setButtonType) {
				return DefaultPathDialog.Action.SET_CURRENT_AS_DEFAULT;
			} else if (buttonType == this.unsetButtonType) {
				return DefaultPathDialog.Action.UNSET_DEFAULT;
			} else {
				throw new AssertionError("Unhandled button type: " + buttonType);
			}
		});
		
		stageManager.register(this);
	}
	
	private void update() {
		if (this.defaultPath == null) {
			this.getDialogPane().setContentText(this.statusWithoutDefault);
			this.getDialogPane().getButtonTypes().setAll(this.setButtonType, ButtonType.CANCEL);
		} else {
			this.getDialogPane().setContentText(String.format(this.statusWithDefault, this.defaultPath));
			this.getDialogPane().getButtonTypes().clear();
			if (!this.defaultPath.equals(this.loadedPath)) {
				this.getDialogPane().getButtonTypes().addAll(this.loadButtonType, this.setButtonType);
			}
			this.getDialogPane().getButtonTypes().addAll(this.unsetButtonType, ButtonType.CANCEL);
		}
	}
	
	public void initStrings(
		final String titleKey,
		final String statusWithDefaultKey,
		final String statusWithoutDefaultKey,
		final String loadButtonKey,
		final String setButtonKey,
		final String unsetButtonKey
	) {
		this.setTitle(i18n.translate(titleKey));
		this.statusWithDefault = i18n.translate(statusWithDefaultKey);
		this.statusWithoutDefault = i18n.translate(statusWithoutDefaultKey);
		this.loadButtonType = new ButtonType(i18n.translate(loadButtonKey));
		this.setButtonType = new ButtonType(i18n.translate(setButtonKey));
		this.unsetButtonType = new ButtonType(i18n.translate(unsetButtonKey));
		this.update();
	}
	
	public void initPaths(final Path loadedPath, final Path defaultPath) {
		this.loadedPath = loadedPath;
		this.defaultPath = defaultPath;
		this.update();
	}
}
