package org.envirocar.obd;

import org.envirocar.obd.commands.response.DataResponse;

public interface DataCollector {

    void receiveDataResponse(DataResponse dr);

}
