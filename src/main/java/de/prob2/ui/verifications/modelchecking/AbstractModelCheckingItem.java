package de.prob2.ui.verifications.modelchecking;


import de.prob2.ui.verifications.Checked;
import javafx.scene.paint.Color;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

public abstract class AbstractModelCheckingItem {

    protected transient FontAwesomeIconView status;

    protected Checked checked;

    public AbstractModelCheckingItem() {
        this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
        this.status.setFill(Color.BLUE);
        this.checked = Checked.NOT_CHECKED;
    }

    public FontAwesomeIconView getStatus() {
        return status;
    }

    public Checked getChecked() {
        return checked;
    }

    public void setCheckedSuccessful() {
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
        icon.setFill(Color.GREEN);
        this.status = icon;
        this.checked = Checked.SUCCESS;
    }

    public void setCheckedFailed() {
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
        icon.setFill(Color.RED);
        this.status = icon;
        this.checked = Checked.FAIL;
    }

    public void setTimeout() {
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
        icon.setFill(Color.YELLOW);
        this.status = icon;
        this.checked = Checked.TIMEOUT;
    }

}
