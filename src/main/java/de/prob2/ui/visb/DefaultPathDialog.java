package de.prob2.ui.visb;

import java.nio.file.Path;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public final class DefaultPathDialog extends Dialog<DefaultPathDialog.Action> {
	public enum Action {
		LOAD_DEFAULT,
		LOAD_DEFINITIONS,
		SET_CURRENT_AS_DEFAULT,
		UNSET_DEFAULT,
	}

	private final SimpleObjectProperty<Path> loadedPath;
	private final SimpleObjectProperty<Path> defaultPath;

	private final ButtonType loadButtonType;
	private final ButtonType loadDefinitionsButtonType;
	private final ButtonType setButtonType;
	private final ButtonType unsetButtonType;

	@Inject
	public DefaultPathDialog(final StageManager stageManager, final I18n i18n) {
		super();

		this.loadedPath = new SimpleObjectProperty<>();
		this.defaultPath = new SimpleObjectProperty<>();

		this.loadButtonType = new ButtonType(i18n.translate("visb.defaultVisualisation.load"));
		this.loadDefinitionsButtonType = new ButtonType(i18n.translate("visb.definitionsVisualisation.load"));
		this.setButtonType = new ButtonType(i18n.translate("visb.defaultVisualisation.set"));
		this.unsetButtonType = new ButtonType(i18n.translate("visb.defaultVisualisation.reset"));

		this.getDialogPane().getButtonTypes().setAll(this.setButtonType, this.loadDefinitionsButtonType, ButtonType.CANCEL);

		this.setTitle(i18n.translate("visb.defaultVisualisation.header"));
		this.setResultConverter(buttonType -> {
			if (buttonType == null || buttonType == ButtonType.CANCEL) {
				return null;
			} else if (buttonType == this.loadButtonType) {
				return DefaultPathDialog.Action.LOAD_DEFAULT;
			} else if (buttonType == this.loadDefinitionsButtonType) {
				return DefaultPathDialog.Action.LOAD_DEFINITIONS;
			} else if (buttonType == this.setButtonType) {
				return DefaultPathDialog.Action.SET_CURRENT_AS_DEFAULT;
			} else if (buttonType == this.unsetButtonType) {
				return DefaultPathDialog.Action.UNSET_DEFAULT;
			} else {
				throw new AssertionError("Unhandled button type: " + buttonType);
			}
		});
		this.contentTextProperty().bind(
			Bindings.when(this.defaultPath.isNull())
				.then(i18n.translate("visb.noDefaultVisualisation.text"))
				.otherwise(
					Bindings.when(this.defaultPath.isEqualTo(loadedPath))
						.then(i18n.translateBinding("visb.currentAsDefaultVisualisation.text", this.defaultPath))
						.otherwise(i18n.translateBinding("visb.defaultVisualisation.text", this.defaultPath))
				)
		);
		ChangeListener<? super Path> changeListener = (observable, oldValue, newValue) -> {
			this.getDialogPane().getButtonTypes().clear();
			if (this.defaultPath.get() == null) {
				this.getDialogPane().getButtonTypes().add(this.setButtonType);
			} else {
				if (!this.defaultPath.get().equals(this.loadedPath.get())) {
					this.getDialogPane().getButtonTypes().addAll(this.loadButtonType, this.setButtonType);
				}
				this.getDialogPane().getButtonTypes().add(this.unsetButtonType);
			}
			if (!VisBController.NO_PATH.equals(this.loadedPath.get())) {
				this.getDialogPane().getButtonTypes().add(this.loadDefinitionsButtonType);
			}
			this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		};
		this.defaultPath.addListener(changeListener);
		this.loadedPath.addListener(changeListener);

		stageManager.register(this);
	}


	public void initPaths(final Path loadedPath, final Path defaultPath) {
		this.loadedPath.set(loadedPath);
		this.defaultPath.set(defaultPath);
	}
}
