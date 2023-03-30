package de.prob2.ui.codecompletion;

import java.util.Optional;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.stage.Window;

public interface ParentWithEditableText {

	Window getWindow();

	ObservableValue<Optional<Point2D>> getCaretPosition();

	ObservableValue<Optional<String>> getTextBeforeCaret();

}
