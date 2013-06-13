package org.reactome.CS.x.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;



public class GSMATaskFactory extends AbstractTaskFactory
{

    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new GSMATask());
    }

}
