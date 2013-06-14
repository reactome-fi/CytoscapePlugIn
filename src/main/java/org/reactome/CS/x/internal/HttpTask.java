package org.reactome.CS.x.internal;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;







public class HttpTask extends AbstractTask
{


    HttpTask()
    {

    }


    @Override
    public void run(TaskMonitor arg0) throws Exception
    {
	// TODO Auto-generated method stub
	String url = "www.google.ca";
	System.out.println(url);

    }
}