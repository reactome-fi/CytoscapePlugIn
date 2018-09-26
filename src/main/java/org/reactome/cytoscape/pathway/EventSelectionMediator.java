/*
 * Created on Jul 30, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.ArrayList;
import java.util.List;

/**
 * To triage event selection among Event tree, multiple pathway internal frames.
 * @author gwu
 *
 */
public class EventSelectionMediator {
    private List<EventSelectionListener> listeners;
    // A lock
    private boolean lock;
    
    public EventSelectionMediator() {
    }

    /**
     * Register an EventSelectionListener.
     * @param l
     */
    public void addEventSelectionListener(EventSelectionListener l) {
        if (listeners == null)
            listeners = new ArrayList<EventSelectionListener>();
        if (!listeners.contains(l))
            listeners.add(l);
    }
    
    /**
     * Remove a registered EventSelectionListener.
     * @param l
     */
    public void removeEventSelectionListener(EventSelectionListener l) {
        if (listeners == null)
            return;
        listeners.remove(l);
    }
    
    /**
     * Propagate an EventSelectionListener from the passed EventSelectionListener to
     * other registered listener..
     * @param l
     */
    public synchronized void propageEventSelectionEvent(EventSelectionListener l,
                                           EventSelectionEvent event) {
        if (lock)
            return;
        lock = true;
        if (listeners == null) {
            lock = false;
            return;
        }
        for (EventSelectionListener listener : listeners) {
            if (listener == l)
                continue;
            listener.eventSelected(event);
        }
        lock = false;
    }
    
}
