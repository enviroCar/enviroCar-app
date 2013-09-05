/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.event;

/**
 * super-interface for an event listener.
 * The current {@link EventBus} implementation relies on every
 * sub-interface to provide an annotation {@link SupportedEventClass}
 * which defines the supported Event type Class.
 * 
 * @author matthes rieke
 *
 * @param <T>
 */
public interface EventListener<T> {
	
	/**
	 * @param event the event holding its payload
	 */
	public void receiveEvent(T event);
	
}
