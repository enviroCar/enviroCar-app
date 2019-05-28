package org.envirocar.core.logging;

import android.content.Context;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

import javax.annotation.Nonnull;
/*
    Sender Factory required as of ACRA 5.*
*/
public class ACRASenderFactory implements ReportSenderFactory {

    public ACRASenderFactory() {
    }


    @Override
    public ReportSender create(Context context, org.acra.config.CoreConfiguration config) {
        return new ACRACustomSender();
    }

    @Override
    public boolean enabled(@Nonnull CoreConfiguration coreConfig) {
        return true;
    }
}