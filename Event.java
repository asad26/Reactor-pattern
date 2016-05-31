package reactor;

import reactorapi.EventHandler;

public class Event<T> {
	private final T event;
	private final EventHandler<T> handler;

	public Event(T e, EventHandler<T> eh) {
		event = e;
		handler = eh;
	}

	public synchronized T getEvent() {
		return event;
	}

	public synchronized EventHandler<T> getHandler() {
		return handler;
	}

	public synchronized void handle() {
		handler.handleEvent(event);
	}
}