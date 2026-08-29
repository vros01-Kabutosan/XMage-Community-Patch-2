/*
 * Decompiled with CFR.
 */
package mage.client.game;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingWorker;
import mage.client.SessionHandler;
import mage.view.MatchView;
import org.apache.log4j.Logger;

class ReplayTask
extends SwingWorker<Void, Collection<MatchView>> {
    private final UUID gameId;
    private static final Logger logger = Logger.getLogger(ReplayTask.class);

    ReplayTask(UUID gameId) {
        this.gameId = gameId;
    }

    @Override
    protected Void doInBackground() throws Exception {
        while (!this.isCancelled()) {
            SessionHandler.nextPlay(this.gameId);
            TimeUnit.SECONDS.sleep(1L);
        }
        return null;
    }

    @Override
    protected void done() {
        try {
            this.get();
        }
        catch (InterruptedException | ExecutionException ex) {
            logger.fatal((Object)"Replay Match Task error", (Throwable)ex);
        }
        catch (CancellationException cancellationException) {
            // empty catch block
        }
    }
}
