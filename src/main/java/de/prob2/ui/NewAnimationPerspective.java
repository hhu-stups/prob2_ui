package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

@Singleton
public class NewAnimationPerspective extends SplitPane{
    @FXML
    private OperationsView operations;
    @FXML
    private HistoryView history;
    @FXML
    private DottyView dotty;
    @FXML
    private ModelcheckingController modelcheck;
    private Parent root;

    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            /*parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent previousParent, Parent nextParent)-> {
                root = nextParent;
                onDrag();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {}

    /*private void onDrag() {
        this.setOnDragDetected((MouseEvent t) -> {
            System.out.println("dragged");
        });
    }*/
}
