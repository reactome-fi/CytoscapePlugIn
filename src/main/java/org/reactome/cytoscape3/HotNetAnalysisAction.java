package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class HotNetAnalysisAction extends FICytoscapeAction
{

    public HotNetAnalysisAction(String title)
    {
        super(title);

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // TODO Auto-generated method stub
        
    }
    
    class HotNetAnalysisTaskFactory extends AbstractTaskFactory
    {

        @Override
        public TaskIterator createTaskIterator()
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    class HotNetAnalysisTask extends AbstractTask
    {

        @Override
        public void run(TaskMonitor arg0) throws Exception
        {
            // TODO Auto-generated method stub
            
        }
        
    }
}
