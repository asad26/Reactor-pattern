package reactor;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import reactorapi.*;

public class Dispatcher {
	
	private List<Combine> registeredHandlers = new ArrayList<Combine>();
	private BlockingEventQueue<Object> buffer;
	private Event<?> event;
	
	public Dispatcher() {
		this(10);
		buffer = new BlockingEventQueue<Object>(10);
	}

	
	public Dispatcher(int capacity) {
		buffer = new BlockingEventQueue<Object>(capacity);
	}

	
	/* Handling events by dispatching it to its corresponding event handler */
	public void handleEvents() throws InterruptedException {
		
		/* It will run until there are registered handlers present in a system */
		while (!registeredHandlers.isEmpty()) {
			try {
				event = select();
			} catch (IOException e) {
				return;
			}
			
			/* Handle that event by sending it to its event handler */
			event.handle();
		} 
	}

	/* This is continuously getting events from the queue and return it to handleEvents method */ 
	public Event<?> select() throws InterruptedException, IOException {
		return buffer.get();
	}

	/* This method register the handler so that events would be dispatched to it */
	public <T> void addHandler(EventHandler<T> h) {
		
		/* First creating a thread then store it together with its event handler 
		 * using a Combine class and then add it in an array list.
		 * After that the thread is started */
		WorkerThread<T> wThread = new WorkerThread<T>(h, buffer);
		Combine c = new Combine(wThread, h);	// User-defined class object 'c' used to store thread and handler together
		registeredHandlers.add(c);
		wThread.start();
	}

	/* This method removes the particular handle from the application */
	public <T> void removeHandler(EventHandler<T> h) {
		
		/* This first finds the handler in a list and then stop the corresponding thread
		 * which was working on that handler to receive events */
		for (int i = 0; i < registeredHandlers.size(); i++) {
			Combine c = registeredHandlers.get(i);
			if (h.equals(c.getHandler())) {
				c.getThread().cancelThread();
				registeredHandlers.remove(i);
			}
		}
	}

	/* Internal representation of a class used to store thread and event handler together */
	public class Combine {
		
		private WorkerThread<?> wthread;
		private EventHandler<?> ehandler;
		
		/* Create a new combination of event handler and the thread which is working on it */
		public Combine(WorkerThread<?> wthread, EventHandler<?> ehandler) {
			this.wthread = wthread;
			this.ehandler = ehandler;
		}
		
		/* Get the current thread */
		public WorkerThread<?> getThread() {
			return wthread;
		}
		
		/* Get the event handler of the corresponding thread */
		public EventHandler<?> getHandler() {
			return ehandler;
		}
	}
	
}
