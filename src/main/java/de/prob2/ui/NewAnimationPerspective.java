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
    @FXML
    private Accordion accordion;
    private Dragboard db;
    @Inject
    public NewAnimationPerspective() {
        try {
            FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent previousParent, Parent nextParent)-> {
                onDrag();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@FXML
	public void initialize() {
		//this.setDividerPositions(0.3);
        double initialSize = 250;
        operations.setPrefSize(initialSize,initialSize);
        history.setPrefSize(initialSize,initialSize);
        modelcheck.setPrefSize(initialSize,initialSize);
        animations.setPrefSize(initialSize,initialSize);
	}

	private void onDrag() {
        this.setOnMouseEntered(s -> this.setCursor(Cursor.OPEN_HAND));
        this.setOnMousePressed(s -> this.setCursor(Cursor.CLOSED_HAND));
        this.setOnMouseReleased(s -> this.setCursor(Cursor.DEFAULT));
        this.setOnDragOver(t -> {
            System.out.println("Drag over");
        });
        operations.setOnDragEntered(t ->{
            System.out.println("Drag entered");
            //db = t.getDragboard();
            t.consume();
        });
        operations.setOnDragDropped(t -> {
            System.out.println("Drag dropped");
            t.consume();
        });
        operations.setOnDragExited(t -> {
            System.out.println("Drag exited");
            dragDropped(operations,operationsTP);
            t.consume();
        });
        operations.setOnDragDetected(s -> {
            System.out.println("Drag detected");
            operations.startFullDrag();
            //dragDropped(operations,operationsTP);
            s.consume();
        });
        history.setOnDragDetected(s -> {
            //dragDropped(history,historyTP);
        });
        modelcheck.setOnDragDetected(s -> {
            //dragDropped(modelcheck,modelcheckTP);
        });
        animations.setOnDragDetected(s -> {
            //dragDropped(animations,animationsTP);
        });
	}

    private void dragDropped(final Node node, final TitledPane nodeTP){
        System.out.println(node.getClass().toString() + " dragged, isResizable() = "+node.isResizable());
        SnapshotParameters snapParams = new SnapshotParameters();
        snapParams.setFill(Color.TRANSPARENT);
        db = node.startDragAndDrop(TransferMode.ANY);
        db.setDragView(node.snapshot(snapParams, null));
        if (vBox.getChildren().contains(node)) {
            if (this.getRight() == null) {
                node.resize(0,0);
                this.setRight(node);
            } else if (this.getTop() == null) {
                node.resize(0,0);
                this.setTop(node);
            } else if (this.getBottom() == null) {
                node.resize(0,0);
                this.setBottom(node);
            } else if (this.getLeft() == null) {
                node.resize(0,0);
                this.setLeft(node);
            }
            vBox.getChildren().remove(node);
            System.out.println("Width = "+node.getBoundsInParent().getWidth()+", Height = "+node.getBoundsInParent().getHeight());
        } else {
            node.resize(0,0);
            vBox.getChildren().add(node);
            this.getChildren().remove(node);
        }
    }
}
