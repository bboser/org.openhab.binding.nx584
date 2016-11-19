package org.openhab.binding.nx584.internal.panel.util;


/**	A synchronized FIFO for interprocess communication.
	See P. Hyde, Java Thread Programming, SAMS 1999, chapter 18, pp. 441.
 * @param <T>
*/

public class FIFO<T> {
	
	private final T[] queue;
	private final int capacity;
	private int size;
	private int head;
	private int tail;
	
	/** Create a new FIFO with the specified capacity.
		@param cap capacity. Cannot be changed after construction.
	*/
	public FIFO(int cap) {
		capacity = cap > 0 ? cap : 1;
		queue = (T[])(new Object[capacity]);
		head = tail = size = 0;
	}	// constructor
	
	/** Maximum number of objects this FIFO can hold.
          * @return  */
	public int getCapacity() { return capacity; }
	
	/** Actual number of objects currently in this FIFO.
          * @return  */
	synchronized public int size() { return size; }
	
	synchronized public boolean isEmpty() { return size == 0; }
	
	synchronized public boolean isFull() { return size == capacity; }
	
	/** Add a new object to this FIFO.
		If the FIFO is currently full, this method blocks (indefinitely) until 
		a different thread removes an element from the FIFO.
          * @param obj
          * @throws java.lang.InterruptedException
	*/
	synchronized public void add(T obj) throws InterruptedException {
		waitWhileFull();
		queue[head] = obj;
		head = (head + 1) % capacity;
		size++;
		notifyAll();		// let all waiting threads (if any) know about the change
	}	// add
	
	/** Add a list of objects.
		Calling this method guarantees that all objects are added in the the order
		specified and not interspersed with objects added by other threads.
	*/
	synchronized public void add(T obj[]) throws InterruptedException {
		// this could be more efficiently implemented ...
		for (int i=0;  i<obj.length;  i++) add(obj[i]);
	}	// add
	
	/** Add message to beginning of FIFO. */
	synchronized public void prepend(T o) throws InterruptedException {
		waitWhileFull();
		tail--;
		if (tail < 0) tail = queue.length - 1;
		queue[tail] = o;
		size++;
		notifyAll();
	}	// prepend
	
	/** Remove one object from the FIFO. Wait (indefinitely) until an object becomes available. */
	synchronized public T remove() throws InterruptedException {
		waitWhileEmpty();
		T obj = queue[tail];
		queue[tail] = null;			// allow GC to collect the object once the caller releases it also
		tail = (tail + 1) % capacity;
		size--;
		return obj;
	}	// remove
	
	/** Remove all objects from the FIFO. If the FIFO is empty, an empty list is returned. */
	synchronized public T[] removeAll() throws InterruptedException { 
		// this could be more efficiently implemented ...
		T obj[] = (T[])(new Object[size]);
		for (int i=0;  i<obj.length;  i++) obj[i] = remove();
		return obj;
	}	// removeAll
	
	/** Remove all objects from the FIFO. If the FIFO is empty, this method waits until an element is added. */
	synchronized public T[] removeAtLeastOne() throws InterruptedException {
		waitWhileEmpty();		// wait for at least one element
		return removeAll();
	}	// removeAtLeastOne
	
	/** Next object returned by remove or null if FIFO is empty. */
	synchronized public T peek() throws InterruptedException {
		return isEmpty() ? null : queue[tail];
	}	// peek
	
	synchronized public boolean waitUntilEmpty(long timeout) throws InterruptedException {
		if (timeout == 0) { waitUntilEmpty();  return true; }
		long endTime = System.currentTimeMillis() + timeout;
		long remainingTime = timeout;
		// wait no longer than the specified time
		while (isEmpty() && remainingTime > 0) {
			wait(remainingTime);
			remainingTime = endTime - System.currentTimeMillis();
		}
		// may have timed out or met condition
		return isEmpty();
	}	// waitUntilEmpty
	
	synchronized public void waitUntilEmpty() throws InterruptedException {
		while (!isEmpty()) wait();
	}	// waitUntilEmpty
		
	synchronized public void waitWhileEmpty() throws InterruptedException {
		while (isEmpty()) wait();
	}	// waitWhileEmpty
		
	synchronized public void waitUntilFull() throws InterruptedException {
		while (!isFull()) wait();
	}	// waitUntilFull
		
	synchronized public void waitWhileFull() throws InterruptedException {
		while (isFull()) wait();
	}	// waitWhileFull
	
	///////////////////////////////////////////////////////////////////////////////////
	// testing

	/*
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append(">>>>>>>>>>>>>> FIFO\n");
		if (isEmpty()) {
			b.append("empty\n");
		} else {
			int x = tail;
			for (int i=0;  i<size();  i++) {
				b.append(queue[x]).append('\n');
				x = (x + 1) % capacity;
			} 
		}
		b.append("<<<<<<<<<<<<<<\n");
		return b.toString();
	}	// toString
	
	public static void main(String args[]) throws Exception {
		FIFO fifo = new FIFO(5);
		println(fifo);
		fifo.prepend("prepend on empty");
		println(fifo);
		for (int i=0;  i<3;  i++) fifo.add("a " + i);
		println(fifo);
		println("remove = " + fifo.remove());
		fifo.prepend("prepend partially full");
		println(fifo);
		println("fifo.peek " + fifo.peek());
		println("remove = " + fifo.remove());
		println("remove = " + fifo.remove());
		println("remove = " + fifo.remove());
		int i = 0;
		while (!fifo.isFull()) fifo.add("b " + i++);
		println(fifo);
		println("remove = " + fifo.remove());
		println(fifo);
		fifo.prepend("prepend almost full");
		println(fifo);
		println("remove = " + fifo.remove());
		println("remove = " + fifo.remove());
		println("remove = " + fifo.remove());
		println("remove = " + fifo.remove());				
	}	// main
	
	private static void println(Object o) { System.out.println(o); }
	*/
		
}	// FIFO

