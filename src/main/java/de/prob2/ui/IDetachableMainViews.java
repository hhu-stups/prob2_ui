package de.prob2.ui;

import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

import java.util.Map;

public interface IDetachableMainViews {
    Map<TitledPane,Accordion> getParentMap();
}
