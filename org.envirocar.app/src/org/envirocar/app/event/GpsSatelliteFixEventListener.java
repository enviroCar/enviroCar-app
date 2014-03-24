package org.envirocar.app.event;

@SupportedEventClass(supportedClass = GpsSatelliteFixEvent.class)
public interface GpsSatelliteFixEventListener extends EventListener<GpsSatelliteFixEvent> {

	@Override
	public void receiveEvent(GpsSatelliteFixEvent event);

}
