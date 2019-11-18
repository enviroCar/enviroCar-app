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
package org.envirocar.obd.adapter.async;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;

public class CarriageReturnCommand implements BasicCommand {

    private static final Logger LOG = Logger.getLogger(CarriageReturnCommand.class);

	@Override
	public byte[] getOutputBytes() {

        try {
            Thread.sleep(750);
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
        }

        return new byte[0];
	}

	@Override
	public boolean awaitsResults() {
		return true;
	}
}
