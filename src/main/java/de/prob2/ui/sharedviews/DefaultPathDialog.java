package de.prob2.ui.sharedviews;

import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public final class DefaultPathDialog extends Dialog<DefaultPathDialog.Action> {
	public enum Action {
		LOAD_DEFAULT,
		SET_CURRENT_AS_DEFAULT,
		UNSET_DEFAULT,
	}
	
	private final ResourceBundle bundle;
	
	private String statusWithDefault;
	private String statusWithoutDefault;
	private ButtonType loadButtonType;
	private ButtonType setButtonType;
	private ButtonType unsetButtonType;
	private Path loadedPath;
	private Path defaultPath;
	
	@Inject
	public DefaultPathDialog(final StageManager stageManager, final ResourceBundle bundle) {
		super();
		
		this.bundle = bundle;
		
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
		this.setTitle(bundle.getString(titleKey));
		this.statusWithDefault = bundle.getString(statusWithDefaultKey);
		this.statusWithoutDefault = bundle.getString(statusWithoutDefaultKey);
		this.loadButtonType = new ButtonType(bundle.getString(loadButtonKey));
		this.setButtonType = new ButtonType(bundle.getString(setButtonKey));
		this.unsetButtonType = new ButtonType(bundle.getString(unsetButtonKey));
		this.update();
	}
	
	public void initPaths(final Path loadedPath, final Path defaultPath) {
		this.loadedPath = loadedPath;
		this.defaultPath = defaultPath;
		this.update();
	}
}
