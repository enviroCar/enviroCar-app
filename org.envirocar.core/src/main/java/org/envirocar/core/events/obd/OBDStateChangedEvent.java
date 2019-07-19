package org.envirocar.core.events.obd;

import com.google.common.base.MoreObjects;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class OBDStateChangedEvent {

    public enum State {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    private final State state;

    /**
     * Constructor.
     *
     * @param state of the OBD connection.
     */
    public OBDStateChangedEvent(final State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("state", state)
                .toString();
    }
}
