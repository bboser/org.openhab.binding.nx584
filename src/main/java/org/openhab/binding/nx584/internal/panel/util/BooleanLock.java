package org.openhab.binding.nx584.internal.panel.util;

/**	A BooleanLock interprocess communication.
	See P. Hyde, Java Thread Programming, SAMS 1999, chapter 17, pp. 408.
*/

public class BooleanLock {
	
	private boolean value;
	
	public BooleanLock(boolean initialValue) {
		value = initialValue;
	}	// constructor
	
	synchronized public void setValue(boolean newValue) {
		if (newValue != value) {
			value = newValue;
			notifyAll();
		}
	}	// setValue
	
	/** Checks if the lock is true. */
	synchronized public boolean isTrue() { return value; }
	
	/** Checks if the lock is false. */
	synchronized public boolean isFalse() { return !value; }
	
	/**	Makes sure that the lock is false (wait if necessary), and then set it to true.
		@param timeout Timeout in milliseconds. Specify 0 to wait indefinitely if necessary.
		@return True if the lock was set to true from an initial value of false.
	*/
	synchronized public boolean waitToSetTrue(long timeout) {
		boolean success = waitUntilFalse(timeout);
		if (success) setValue(true);
		return success;
	}	// waitToSetTrue
	
	/**	Makes sure that the lock is true (wait if necessary), and then set it to false.
		@param timeout Timeout in milliseconds. Specify 0 to wait indefinitely if necessary.
		@return True if the lock was set to false from an initial value of true.
	*/
	synchronized public boolean waitToSetFalse(long timeout) {
		boolean success = waitUntilTrue(timeout);
		if (success) setValue(false);
		return success;
	}	// waitToSetFalse
	
	/** Wait until the lock is true.
		@param timeout Timeout in milliseconds. Specify 0 to wait indefinitely if necessary.
		@return True if the lock is true, false if the operation timed out.
	*/
	synchronized public boolean waitUntilTrue(long timeout) {
		return waitUntilStateIs(true, timeout);
	}	// waitUntilTrue
	
	/** Wait until the lock is false.
		@param timeout Timeout in milliseconds. Specify 0 to wait indefinitely if necessary.
		@return True if the lock is false, false if the operation timed out.
	*/
	synchronized public boolean waitUntilFalse(long timeout) {
		return waitUntilStateIs(false, timeout);
	}	// waitUntilFalse
	
	/**	Wait until lock has required state up to specified timeout.
		@param state State the lock should have after returning.
		@param timeout Maximum time in milliseconds to wait. Specify 0 to wait indefinitely.
		@return True if the lock has the requested value.
	*/
	synchronized public boolean waitUntilStateIs(boolean state, long timeout) {
		if (timeout == 0) {
			// wait indefinitely until notified
			while (value != state) {
			    try { wait(); } catch (InterruptedException ie) {}
                        }
			return true;		// we succeeded
		}
		
		// only wait for the specified amount of time
		long endTime = System.currentTimeMillis() + timeout;
		long remainingTime = timeout;
		while (value != state && remainingTime > 0) {
			try { wait(remainingTime); } catch (InterruptedException ie) {}
			remainingTime = endTime - System.currentTimeMillis();
		}
		// may have succeeded or timed out
		return value == state;
	}	// waitUnitlStateIs
	
}	// BooleanLock
