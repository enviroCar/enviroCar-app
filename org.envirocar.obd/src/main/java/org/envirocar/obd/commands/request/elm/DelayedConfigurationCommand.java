/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.commands.request.elm;

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
