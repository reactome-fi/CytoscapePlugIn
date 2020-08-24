/*
 * Created on Sep 28, 2016
 *
 */
package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Show popup menu for Reaction networks.
 * @author gwu
 *
 */
public class ScNetworkPopupMenuHandler extends FINetworkPopupMenuHandler {
    
    public ScNetworkPopupMenuHandler() {
    }
    
    private ActionListener createActionListener(Consumer<ActionEvent> funct,
                                                String title) {
        ActionListener l = e -> {
            Task task = new AbstractTask() {

                @Override
                public void run(TaskMonitor taskMonitor) throws Exception {
                    taskMonitor.setTitle(title);
                    funct.accept(e);
                }
            };
            TaskManager<?, ?> taskManager = PlugInObjectManager.getManager().getTaskManager();
            taskManager.execute(new TaskIterator(task));
        };
        return l;
    }

    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        
        Properties props = new Properties();
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, 
                     new GeneExpressionMenu(), 
                     CyNetworkViewContextMenuFactory.class, 
                     props);
        
        // Add menus for loading cell features
        List<String> cellFeatures = ScNetworkManager.getManager().getCellFeatureNames();
        if (cellFeatures != null && cellFeatures.size() > 0) {
            props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU + ".Load Cell Feature[5.0]");
            for (String feature : cellFeatures) {
                CyNetworkViewContextMenuFactory menuFactory = view -> {
                    JMenuItem menuItem = new JMenuItem(feature);
                    menuItem.addActionListener(e -> ScNetworkManager.getManager().loadCellFeature(feature));
                    return new CyMenuItem(menuItem, cellFeatures.indexOf(feature));
                };
                addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
            }
        }
        
        if (ScNetworkManager.getManager().isForRNAVelocity()) {
            addRNAVelocityMenus(context, props, cellFeatures);
        }
        
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, new CytotraceAnalysis(), CyNetworkViewContextMenuFactory.class, props);
        addPopupMenu(context, new DPTAnalysis(), CyNetworkViewContextMenuFactory.class, props);
        addPopupMenu(context, new DifferentialExpressionAnalysisMenu(), CyNetworkViewContextMenuFactory.class, props);
        addPopupMenu(context, new ProjectMenuItem(), CyNetworkViewContextMenuFactory.class, props);
        addPopupMenu(context, new ToggleProjectedCells(), CyNetworkViewContextMenuFactory.class, props);

        addPopupMenu(context, 
                     new ToggleEdgesDisplay(),
                     CyNetworkViewContextMenuFactory.class,
                     props);
        addPopupMenu(context, 
                     new SaveMenuItem(),
                     CyNetworkViewContextMenuFactory.class,
                     props);
        
    }
    
    private String getGene() {
        String gene = JOptionPane.showInputDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  "Enter a gene:",
                                                  "Enter Gene",
                                                  JOptionPane.PLAIN_MESSAGE);
        return gene;
    }

    public void addRNAVelocityMenus(BundleContext context, Properties props, List<String> cellFeatures) {
        // Add RNA velocity related feature
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU + ".RNA Velocity Analysis[5.5]");
        // Three embeddings
        CyNetworkViewContextMenuFactory menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Embedding");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().showScvEmbedding("scv_embedding"));
            return new CyMenuItem(menuItem, 0.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
        menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Embedding Grid");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().showScvEmbedding("scv_embedding_grid"));
            return new CyMenuItem(menuItem, 1.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
        menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Embedding Stream");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().showScvEmbedding("scv_embedding_stream"));
            return new CyMenuItem(menuItem, 2.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
        // Gene-based data
        menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Gene Velocity");
            menuItem.addActionListener(e -> {
                String gene = getGene();
                if (gene == null)
                    return;
                ScNetworkManager.getManager().showGeneVelocity(gene);
            });
            return new CyMenuItem(menuItem, 3.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
        menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Rank Velocity Genes");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().rankVelocityGenes());
            return new CyMenuItem(menuItem, 4.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
        menuFactory = view -> {
            JMenuItem menuItem = new JMenuItem("Rank Dynamic Genes");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().rankDynamicGenes());
            return new CyMenuItem(menuItem, 5.0f);
        };
        addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
    }
    
    private class ProjectMenuItem implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem projectMenuItem = new JMenuItem("Project New Data");
            projectMenuItem.addActionListener(e -> ScNetworkManager.getManager().project());
            return new CyMenuItem(projectMenuItem, 15.0f);
        }
        
    }
    
    private class SaveMenuItem implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem projectMenuItem = new JMenuItem("Save Analysis Results");
            projectMenuItem.addActionListener(e -> ScNetworkManager.getManager().saveAnalyzedData());
            return new CyMenuItem(projectMenuItem, 50.0f);
        }
        
    }
    
    private class ToggleProjectedCells implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            if (!ScNetworkManager.getManager().hasProjectedData())
                return null;
            JMenuItem projectMenuItem = new JMenuItem("Toggle Projected Data");
            projectMenuItem.addActionListener(e -> ScNetworkManager.getManager().toggleProjectedData());
            return new CyMenuItem(projectMenuItem, 15.5f);
        }
        
    }
    
    private class DifferentialExpressionAnalysisMenu implements CyNetworkViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(CyNetworkView view) {
            JMenuItem menuItem = new JMenuItem("Differential Expression Analysis");
            menuItem.addActionListener(e -> ScNetworkManager.getManager().doDiffExpAnalysis());
            return new CyMenuItem(menuItem, 10.0f);
        }
    }
    
    private class GeneExpressionMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem loadGeneMenuItem = new JMenuItem("Load Gene Expression");
            loadGeneMenuItem.addActionListener(e -> loadGeneExpression());
            return new CyMenuItem(loadGeneMenuItem, 1.0f);
        }
        
        private void loadGeneExpression() {
            String gene = getGene();
            if (gene == null)
                return;
            ScNetworkManager.getManager().loadGeneExp(gene);
        }
        
    }
    
    private class ToggleEdgesDisplay implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            Boolean isEdgeDisplayed = ScNetworkManager.getManager().isEdgeDisplayed();
            if (isEdgeDisplayed == null)
                return null; // Don't need to show this menu
            String title = null;
            if (isEdgeDisplayed)
                title = "Hide Edges";
            else
                title = "Show Edges";
            JMenuItem netPathMenuItem = new JMenuItem(title);
            netPathMenuItem.addActionListener(e -> ScNetworkManager.getManager().setEdgesVisible(!isEdgeDisplayed));
            return new CyMenuItem(netPathMenuItem, 20.0f);
        }
        
    }
    
    private class DPTAnalysis implements CyNetworkViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netPathMenuItem = new JMenuItem("Diffusion Pseudotime Analysis");
            netPathMenuItem.addActionListener(e -> ScNetworkManager.getManager().performDPT());
            return new CyMenuItem(netPathMenuItem, 7.0f);
        }
    }
    
    private class CytotraceAnalysis implements CyNetworkViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem menuItem = new JMenuItem("CytoTrace Analysis");
            menuItem.addActionListener(createActionListener(e -> ScNetworkManager.getManager().performCytoTrace(),
                                                            menuItem.getText()));
            return new CyMenuItem(menuItem, 6.0f);
        }
    }
    
    
    
}
