package de.prob2.ui.helpsystem;

import com.google.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.web.WebView;

@Singleton
public class HelpSystem extends SplitPane{
    @FXML private TopicTree topicTree;
    @FXML private WebView webView;
}
