package reactor;

import reactorapi.*;

public class WorkerThread<T> extends Thread {
	
	private final EventHandler<T> handler;
	private final BlockingEventQueue<Object> queue;
	private volatile boolean exit = false; // This variable is used in order to stop the thread

	public WorkerThread(EventHandler<T> eh, BlockingEventQueue<Object> q) {
		handler = eh;
		queue = q;
	}

	public void run() {
		while(!exit) {
			Event<T> event = new Event<T>(handler.getHandle().read(), handler);
			
			try {
				/* The event is put in a queue until cancelThread method is called */
				if (!exit) {
					queue.put(event);
				}
				
				/* It would stop the loop so that no more events can be put in a queue */
				if (event.getEvent() == null) {
					exit = true;
				}
			} catch (InterruptedException ie) {
				return;
			}
		}
	}

	/* This is used to safely stop the thread */
	public void cancelThread() {
		exit = true;
		/* This would interrupt the thread blocked on a Handle read */
		this.interrupt();
	}
}