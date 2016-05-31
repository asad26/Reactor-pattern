package reactor;

import reactorapi.BlockingQueue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BlockingEventQueue<T> implements BlockingQueue<Event<? extends T>> {
	
	private final int capacity;
	private Queue<Event<? extends T>> queue;
	
	public BlockingEventQueue(int capacity) {
		this.capacity = capacity;
		queue = new LinkedList<Event<? extends T>>();
	}

	public int getSize() {
		synchronized(queue) {
			return queue.size();
		}
	}

	public int getCapacity() {
		synchronized(queue) {
			return this.capacity;
		}
	}

	public Event<? extends T> get() throws InterruptedException {
		Event<? extends T> event = null;
		synchronized(queue) {
			while (queue.isEmpty()) {
				queue.wait();
			}
			event = queue.remove();		// Removes from the head of the queue
			queue.notifyAll();
		}
		return event;
	}

	public List<Event<? extends T>> getAll(){
		synchronized(queue) {
			List<Event<? extends T>> list = new LinkedList<Event<? extends T>>(queue);
			queue.clear();
			queue.notifyAll();
			return list;
		}
	}

	public void put(Event<? extends T> event) throws InterruptedException {
		synchronized(queue) {
			while (queue.size() >= capacity) {
				queue.wait();
			}
			queue.add(event);		// Insert at the end of the list
			queue.notifyAll();
		}
	}
}