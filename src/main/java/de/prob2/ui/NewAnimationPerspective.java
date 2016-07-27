package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;

import java.io.IOException;

@Singleton
public class NewAnimationPerspective extends SplitPane{
    @Inject
    private NewAnimationPerspective(FXMLLoader loader) {
        try {
            loader.setLocation(getClass().getResource("new_animation_perspective.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
