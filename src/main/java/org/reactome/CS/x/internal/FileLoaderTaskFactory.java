package org.reactome.CS.x.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class FileLoaderTaskFactory extends AbstractTaskFactory
{

    private String format;
    public FileLoaderTaskFactory(String format)
    {
	this.format=format;
    }
    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new FileLoaderTask(format));
    }

}
