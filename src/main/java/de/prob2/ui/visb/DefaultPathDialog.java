package de.prob2.ui.visb;

import java.nio.file.Path;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public final class DefaultPathDialog extends Dialog<DefaultPathDialog.Action> {
	public enum Action {
		LOAD_DEFAULT,
		SET_CURRENT_AS_DEFAULT,
		UNSET_DEFAULT,
	}

	private final I18n i18n;
	private final SimpleStringProperty titleKey;
	private final SimpleStringProperty statusWithDefaultKey;
	private final SimpleStringProperty statusWithCurrentAsDefaultKey;
	private final SimpleStringProperty statusWithoutDefaultKey;
	private final SimpleObjectProperty<Path> loadedPath;
	private final SimpleObjectProperty<Path> defaultPath;

	private ButtonType loadButtonType;
	private ButtonType setButtonType;
	private ButtonType unsetButtonType;

	@Inject
	public DefaultPathDialog(final StageManager stageManager, final I18n i18n) {
		super();

		this.i18n = i18n;
		this.titleKey = new SimpleStringProperty("visb.defaultVisualisation.header");
		this.statusWithDefaultKey = new SimpleStringProperty("visb.defaultVisualisation.text");
		this.statusWithCurrentAsDefaultKey = new SimpleStringProperty("visb.currentAsDefaultVisualisation.text");
		this.statusWithoutDefaultKey = new SimpleStringProperty("visb.noDefaultVisualisation.text");
		this.loadedPath = new SimpleObjectProperty<>();
		this.defaultPath = new SimpleObjectProperty<>();

		this.loadButtonType = new ButtonType(i18n.translate("visb.defaultVisualisation.load"));
		this.setButtonType = new ButtonType(i18n.translate(	"visb.defaultVisualisation.set"));
		this.unsetButtonType = new ButtonType(i18n.translate("visb.defaultVisualisation.reset"));

		this.getDialogPane().getButtonTypes().setAll(this.setButtonType, ButtonType.CANCEL);

		this.titleProperty().bind((i18n.translateBinding(this.titleKey)));
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
		this.contentTextProperty().bind(Bindings.when(this.defaultPath.isNull())
								.then(i18n.translateBinding(this.statusWithoutDefaultKey))
								.otherwise(
									Bindings.when(this.defaultPath.isEqualTo(loadedPath))
										.then(i18n.translateBinding(this.statusWithCurrentAsDefaultKey, this.defaultPath))
										.otherwise(i18n.translateBinding(this.statusWithDefaultKey, this.defaultPath))
								));
		this.defaultPath.addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				this.getDialogPane().getButtonTypes().setAll(this.setButtonType, ButtonType.CANCEL);
			} else {
				this.getDialogPane().getButtonTypes().clear();
				if (!newValue.equals(this.loadedPath.get())) {
					this.getDialogPane().getButtonTypes().addAll(this.loadButtonType, this.setButtonType);
				}
				this.getDialogPane().getButtonTypes().addAll(this.unsetButtonType, ButtonType.CANCEL);
			}
		});

		stageManager.register(this);
	}


	public void initPaths(final Path loadedPath, final Path defaultPath) {
		this.loadedPath.set(loadedPath);
		this.defaultPath.set(defaultPath);
	}
}
