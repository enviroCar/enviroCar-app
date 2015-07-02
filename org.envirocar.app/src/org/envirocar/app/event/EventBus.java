///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.event;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.envirocar.app.logging.Logger;
//
///**
// * the central event dissemination component. Classes interested
// * in certain events shall register themselves using {@link #registerListener(EventListener)}.
// *
// * Classes providing events shall fire those through {@link #fireEvent(AbstractEvent)}.
// *
// * @author matthes rieke
// *
// */
//public class EventBus {
//
//	protected static final Logger logger = Logger.getLogger(EventBus.class);
//	private static EventBus instance;
//	private Map<Class<?>, List<EventListener<?>>> listeners = new HashMap<Class<?>, List<EventListener<?>>>();
//
//	private EventBus() {
//	}
//
//	public static synchronized EventBus getInstance() {
//		if (instance == null)
//			instance = new EventBus();
//
//		return instance;
//	}
//
//	public void registerListener(EventListener<?> listener) {
//		Class<?> eventType;
//		try {
//			eventType = resolveEventType(listener);
//		} catch (UnsupportedEventListenerException e) {
//			logger.warn(e.getMessage(), e);
//			return;
//		}
//
//		List<EventListener<?>> list = null;
//		if (!listeners.containsKey(eventType)) {
//			list = new ArrayList<EventListener<?>>();
//			listeners.put(eventType, list);
//		} else {
//			list = listeners.get(eventType);
//		}
//
//		list.add(listener);
//	}
//
//	private Class<?> resolveEventType(EventListener<?> listener) throws UnsupportedEventListenerException {
//		Class<?>[] interfaces = listener.getClass().getInterfaces();
//
//		for (Class<?> interf : interfaces) {
//			if (EventListener.class.isAssignableFrom(interf)) {
//				SupportedEventClass anno = interf.getAnnotation(SupportedEventClass.class);
//				if (anno != null) {
//					return anno.supportedClass();
//				}
//			}
//		}
//
//		throw new UnsupportedEventListenerException(String.format("Listener %s is not supported!", listener.getClass()));
//	}
//
//
//	@SuppressWarnings("unchecked")
//	public void fireEvent(AbstractEvent<?> event) {
//		List<EventListener<?>> candidates = this.listeners.get(event.getClass());
//
//		if (candidates == null || candidates.isEmpty()) return;
//
//		for (EventListener<?> eventListener : candidates) {
//			((EventListener<AbstractEvent<?>>) eventListener).receiveEvent(event);
//		}
//	}
//
//	public void unregisterListener(EventListener<?> el) {
//		if (el == null) return;
//
//		Class<?> type;
//		try {
//			type = resolveEventType(el);
//		} catch (UnsupportedEventListenerException e) {
//			return;
//		}
//
//		List<EventListener<?>> candidates = listeners.get(type);
//		candidates.remove(el);
//	}
//
//
//}
