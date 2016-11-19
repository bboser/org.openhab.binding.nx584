package org.openhab.binding.nx584.internal.panel.util;

/** Implements addListener / removeListener methods that use strong (normal) references. */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListenerQueue<Listener> implements Iterable<Listener> {

    protected List<Listener> listeners = null;

    public ListenerQueue() {
    }	// constructor

    /** Add a listener to the queue. */
    public void addListener(Listener l) {
        if (l == null) {
            return;
        }
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }	// addListener

    /** Remove a listener from the queue. */
    public void removeListener(Listener l) {
        if (l == null || listeners == null) {
            return;
        }
        Iterator<Listener> it = listeners.iterator();
        while (it.hasNext()) {
            if (it.next() == l) {
                it.remove();
            }
        }
    }	// removeListener

    /** Call the applicator on each listener. */
    public void apply(ListenerApplicator<Listener> applicator) {
        if (listeners == null) {
            return;
        }
        for (Listener l : listeners) {
            if (l != null) {
                applicator.apply(l);
            }
        }
    }	// apply

    @Override
    public Iterator<Listener> iterator() {
        return listeners.iterator();
    }
    
}	// ListenerQueue

