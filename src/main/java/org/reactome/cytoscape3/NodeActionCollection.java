package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;


public class NodeActionCollection
{

    class GeneCardMenu implements CyNodeViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                final View<CyNode> nodeView)
        {
            JMenuItem geneCardMenuItem = new JMenuItem("Gene Card");
            geneCardMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Long nodeID = nodeView.getModel().getSUID();
                    String id = netView.getModel().getDefaultNodeTable()
                            .getRow(nodeID).get("name", String.class);
                    String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene="
                            + id;
                    PlugInUtilities.openURL(url);
                }

            });
            return new CyMenuItem(geneCardMenuItem, 3.0f);
        }
    }
    class fetchFIsMenu implements CyNodeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeview)
        {
            JMenuItem fetchFIsItem = new JMenuItem("Fetch FIs");
            fetchFIsItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });

            return new CyMenuItem(fetchFIsItem, 1.0f);
        }
        
    }
    class cancerGeneIndexMenu implements CyNodeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0, View<CyNode> arg1)
        {
            JMenuItem getCancerGeneIndexItem = new JMenuItem("Fetch Cancer Gene Index");
            getCancerGeneIndexItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            return null;
        }
        
    }
    class CancerGeneIndexTaskFactory extends AbstractTaskFactory
    {

        private CyNetworkView view;
        CancerGeneIndexTaskFactory(CyNetworkView view)
        {
            super();
            this.view = view;
        }
        @Override
        public TaskIterator createTaskIterator()
        {

            return new TaskIterator(new CancerGeneIndexTask(view));
        }
        
    }
    class CancerGeneIndexTask extends AbstractTask
    {

        private CyNetworkView view;

        CancerGeneIndexTask(CyNetworkView view)
        {
            this.view = view;
        }

        @Override
        public void run(TaskMonitor tm) throws Exception
        {
            try
            {
                NCICancerIndexDiseaseHelper diseaseHelper = new NCICancerIndexDiseaseHelper();
            }
            catch(Exception e)
            {
                System.err.println("NetworkActionCollection.loadCancerGeneIndex(): " + e);
                e.printStackTrace();
                String message = e.getMessage();
                String line = message.split("\n")[0];
                JOptionPane.showMessageDialog(null,
                                              "Error in loading cancer gene index: " + line,
                                              "Error in Loading Cancer Gene Index",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
        
    }

}
