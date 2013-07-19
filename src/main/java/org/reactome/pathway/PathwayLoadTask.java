/*
 * Created on Jul 16, 2013
 *
 */
package org.reactome.pathway;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/**
 * This class is used to load pathway diagram from a Reactome database via RESTful API.
 * @author gwu
 *
 */
public class PathwayLoadTask extends AbstractTask {
    
    
    public PathwayLoadTask() {
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        System.out.println("Loading pathway diagram using PathwayLoadTask!");
    }

    @Override
    public void cancel() {
        
        super.cancel();
    }
    
}
