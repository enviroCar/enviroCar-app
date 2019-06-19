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
package org.envirocar.core.logging;

import android.content.Context;
import android.util.Log;

//import org.acra.collector.CrashReportData;
import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;


import javax.annotation.Nonnull;

public class ACRACustomSender implements ReportSender {

	private static final Logger logger = Logger.getLogger(ACRACustomSender.class);
	
    public ACRACustomSender(){
    }

    @Override
    public void send(Context context, CrashReportData report) {
        try {
            Log.e("acra", "Receiving an app crash: " + report.toJSON());
        }catch (Exception e){
            e.printStackTrace();
        }
    	logger.severe(report.toString());
    	logger.severe("[END OF ACRA REPORT]");
    }
    
}
