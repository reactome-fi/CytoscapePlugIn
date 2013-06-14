package org.reactome.CS.x.internal;


import org.apache.http.client.HttpClient;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class HttpTaskFactory extends AbstractTaskFactory
{



    public HttpTaskFactory()
    {

    }

    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new HttpTask());
    }

}
