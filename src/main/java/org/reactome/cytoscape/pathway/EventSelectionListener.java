/*
 * Created on Jul 30, 2013
 *
 */
package org.reactome.cytoscape.pathway;

/**
 * An interface for handling event selection
 * @author gwu
 *
 */
public interface EventSelectionListener {
    
    public void eventSelected(EventSelectionEvent selectionEvent);
    
}
