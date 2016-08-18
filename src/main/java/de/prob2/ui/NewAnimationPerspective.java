package de.prob2.ui;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.commons.lang.ObjectUtils;

@Singleton
public class NewAnimationPerspective extends BorderPane {
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
    //private boolean invisibleItems;
    private ImageView snapImage = new ImageView();
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

/*public class NewAnimationPerspective extends SplitPane {
	@FXML
	private OperationsView operations;
	@FXML
	private TitledPane operationsTP;
	@FXML
	private HistoryView history;
	@FXML
	private TitledPane historyTP;
	@FXML
	private AnimationsView animations;
	@FXML
	private TitledPane animationsTP;
	@FXML
	private ModelcheckingController modelcheck;
	@FXML
	private TitledPane modelcheckTP;
	@FXML
	private FlowPane test;
	@FXML
	private TitledPane testTP;
	@FXML
	private Accordion accordion;

	@Inject
	public NewAnimationPerspective() {
		try {
			FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
			loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
			parentProperty().addListener((ObservableValue<? extends Parent>
			ov, Parent previousParent, Parent nextParent)-> { onDrag(); });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/

	@FXML
	public void initialize() {
		//this.setDividerPositions(0.3);
	}

	private void onDrag() {
        this.setOnMouseEntered(t -> this.setCursor(Cursor.OPEN_HAND));
        this.setOnMousePressed(t -> this.setCursor(Cursor.CLOSED_HAND));
        this.setOnMouseReleased(t -> this.setCursor(Cursor.DEFAULT));
        operations.setOnDragDetected(s -> {
            dragAction(operations,operationsTP);
        });
        history.setOnDragDetected(s -> {
            dragAction(history,historyTP);
        });
        modelcheck.setOnDragDetected(s -> {
            dragAction(modelcheck,modelcheckTP);
        });
        animations.setOnDragDetected(s -> {
            dragAction(animations,animationsTP);
        });
	}

    private void dragAction(final Node node, final TitledPane nodeTP){
        System.out.println(node.getClass().toString() + " dragged");
        //SnapshotParameters snapParams = new SnapshotParameters();
        //snapParams.setFill(Color.TRANSPARENT);
        //snapImage.setImage(node.snapshot(snapParams, null));
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
            //nodeTP.setContent(null);
            //accordion.getPanes().remove(nodeTP);
            vBox.getChildren().remove(node);
        } else {
            //this.addAccordion();
            //nodeTP.setContent(node);
            vBox.getChildren().add(node);
            this.getChildren().remove(node);
        }
    }
}
