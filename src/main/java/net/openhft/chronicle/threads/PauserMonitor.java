package net.openhft.chronicle.threads;

import net.openhft.chronicle.threads.api.EventHandler;
import net.openhft.chronicle.threads.api.InvalidEventHandlerException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;


/**
 * Created by peter.lawrey on 04/01/2016.
 */
public class PauserMonitor implements EventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PauserMonitor.class);

    private final WeakReference<Pauser> pauser;
    private final String description;
    private final int mills;
    private long nextLongTime = 0;
    private long lastTimePaused = 0;
    private long lastCountPaused = 0;

    public PauserMonitor(Pauser pauser, String description, int seconds) {
        this.pauser = new WeakReference<>(pauser);
        this.description = description;
        this.mills = seconds * 1000;
    }

    @Override
    public boolean action() throws InvalidEventHandlerException {
        long now = System.currentTimeMillis();
        if (nextLongTime > now) {
            return false;
        }
        Pauser pauser = this.pauser.get();
        if (pauser == null)
            throw new InvalidEventHandlerException();
        long timePaused = pauser.timePaused();
        long countPaused = pauser.countPaused();

        if (nextLongTime > 0) {
            long timePausedDelta = timePaused - lastTimePaused;
            long countPausedDelta = countPaused - lastCountPaused;
            if (countPausedDelta > 0) {
                double averageTime = timePausedDelta * 1000 / countPausedDelta / 1e3;
                LOG.info(description + ": avg pause: " + averageTime + " ms, count=" + countPausedDelta);
            } else {
                LOG.info(description + ": count=" + countPausedDelta);
            }
        }
        lastTimePaused = timePaused;
        lastCountPaused = countPaused;
        nextLongTime = now + mills;
        return true;
    }

    @NotNull
    @Override
    public HandlerPriority priority() {
        return HandlerPriority.MONITOR;
    }
}
