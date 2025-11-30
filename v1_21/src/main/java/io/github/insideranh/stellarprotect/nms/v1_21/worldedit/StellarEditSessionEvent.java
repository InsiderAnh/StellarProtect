package io.github.insideranh.stellarprotect.nms.v1_21.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

public class StellarEditSessionEvent {

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getActor() != null && event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
            event.setExtent(new StellarLogger(event.getActor(), event.getWorld(), event.getExtent()));
        }
    }

}
