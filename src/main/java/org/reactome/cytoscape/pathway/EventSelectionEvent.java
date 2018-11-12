/*
 * Created on Jul 30, 2013
 *
 */
package org.reactome.cytoscape.pathway;

/**
 * Record an Event object selection event.
 * @author gwu
 *
 */
public class EventSelectionEvent {
    private Long parentId; // Id for the event has pathway diagram
    private Long eventId; // Id for actual selected event
    private boolean isPathway; // flag indicating if the selected event is a pathway or not.
    
    
    public EventSelectionEvent() {
    }


    public Long getParentId() {
        return parentId;
    }


    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }


    public Long getEventId() {
        return eventId;
    }


    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }


    public boolean isPathway() {
        return isPathway;
    }


    public void setIsPathway(boolean isPathway) {
        this.isPathway = isPathway;
    }
    
    
    
}
