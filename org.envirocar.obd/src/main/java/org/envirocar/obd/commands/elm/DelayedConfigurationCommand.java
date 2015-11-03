package org.envirocar.obd.commands.elm;

import org.envirocar.core.logging.Logger;

/**
 * Created by matthes on 03.11.15.
 */
public class DelayedConfigurationCommand extends ConfigurationCommand {

    private static final Logger LOG = Logger.getLogger(DelayedConfigurationCommand.class);

    private final long delay;

    public DelayedConfigurationCommand(String output, Instance i, boolean awaitsResult, long delay) {
        super(output, i, awaitsResult);
        this.delay = delay;
    }

    @Override
    public byte[] getOutputBytes() {
        if (this.delay > 0) {
            try {
                Thread.sleep(this.delay);
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return super.getOutputBytes();
    }
}
