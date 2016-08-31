package de.prob2.ui;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.commons.lang.ObjectUtils;

@Singleton
public class NewAnimationPerspective extends BorderPane {
    // TODO (real) DragDrop/Docking
    // TODO reverting to accordion
    @FXML
    private OperationsView operations;
    @FXML
    private TitledPane operationsTP;
    @FXML
    private HistoryView history;
    @FXML
    private TitledPane historyTP;
    @FXML
    private ModelcheckingController modelcheck;
    @FXML
    private TitledPane modelcheckTP;
    @FXML
    private AnimationsView animations;
    @FXML
    private TitledPane animationsTP;
    @FXML
    private VBox vBox;
    /*@FXML
    private Accordion accordion;*/
    private boolean dragged;
    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            onDrag();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@FXML
	public void initialize() {
        //System.out.println(this.getChildren().contains(accordion));
        double initialSize = 250;
        operations.setPrefSize(initialSize,initialSize);
        history.setPrefSize(initialSize,initialSize);
        modelcheck.setPrefSize(initialSize,initialSize);
        animations.setPrefSize(initialSize,initialSize);
	}

	private void onDrag() {
        registerDrag(operations,operationsTP);
        registerDrag(history,historyTP);
        registerDrag(modelcheck,modelcheckTP);
        registerDrag(animations,animationsTP);
	}

    private void registerDrag(final Node node, final TitledPane nodeTP){
        node.setOnMouseEntered(s -> this.setCursor(Cursor.OPEN_HAND));
        node.setOnMousePressed(s -> this.setCursor(Cursor.CLOSED_HAND));
        node.setOnMouseDragEntered(s -> {
            dragged = true;
            //System.out.println("Drag entered");
            s.consume();
        });
        node.setOnMouseReleased(s -> {
            this.setCursor(Cursor.DEFAULT);
            if (dragged){
                dragDropped(node,nodeTP);
                //System.out.println("Drag released");
            }
            dragged = false;
            s.consume();
        });
        node.setOnDragDetected(s -> {
            //System.out.println("Drag detected");
            operations.startFullDrag();
            s.consume();
        });
    }

    private void dragDropped(final Node node, final TitledPane nodeTP){
        //System.out.println(node.getClass().toString() + " dragged, isResizable() = "+node.isResizable());
        if (vBox.getChildren().contains(node)) {
            if (this.getRight() == null) {
                this.setRight(node);
            } else if (this.getTop() == null) {
                this.setTop(node);
            } else if (this.getBottom() == null) {
                this.setBottom(node);
            } else if (this.getLeft() == null) {
                this.setLeft(node);
            }
            vBox.getChildren().remove(node);
        } else {
            vBox.getChildren().add(node);
            this.getChildren().remove(node);
        }
        /*if (accordion.getPanes().contains(nodeTP)) {
            if (this.getRight() == null) {
                this.setRight(node);
            } else if (this.getTop() == null) {
                this.setTop(node);
            } else if (this.getBottom() == null) {
                this.setBottom(node);
            } else if (this.getLeft() == null) {
                this.setLeft(node);
            }
            nodeTP.setContent(null);
            accordion.getPanes().remove(nodeTP);
        } else {
            accordion.getPanes().add(nodeTP);
            nodeTP.setContent(node);
            this.getChildren().remove(node);
        }*/
    }
}
