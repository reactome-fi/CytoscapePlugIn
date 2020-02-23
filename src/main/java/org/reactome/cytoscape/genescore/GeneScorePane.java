package org.reactome.cytoscape.genescore;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

public class GeneScorePane extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Gene Scores";
    
    private GeneScoreTablePane tablePane;
    private GeneScoreDistributionPlotPane plotPane;
    private ServiceRegistration serviceRegistration;
    
    public GeneScorePane() {
        init();
    }
    
    public void close() {
        tablePane.close();
        
        getParent().remove(this);
    }
    
    private void init() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        
        tablePane = new GeneScoreTablePane();
        tabbedPane.add("Table View", tablePane);
        plotPane = new GeneScoreDistributionPlotPane();
        tabbedPane.addTab("Plot View", plotPane);
        
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Need to register. Otherwise, it cannot be displayed.
        serviceRegistration = PlugInUtilities.registerCytoPanelComponent(this);
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                    serviceRegistration = null;
                }
                else if (getParent() != null)
                    getParent().remove(GeneScorePane.this);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener, 
                                null);
    }
    
    public void setPathwayGenes(Set<String> genes) {
        tablePane.setPathwayGenes(genes);
    }
    
    public void setGeneToScore(Map<String, Double> geneToScore) {
        tablePane.setGeneToScore(geneToScore);
    }
    
    public void setGeneToScore(Map<String, Double> geneToScore, Set<String> pathwayGenes) {
        plotPane.setGeneToScore(geneToScore, pathwayGenes);
    }
    
    public void setGeneToDBIDs(Map<String, List<Long>> geneToDBIDs) {
        tablePane.setGeneToDBIDs(geneToDBIDs);
    }
    
    public void setDBIDToGenes(Map<Long, Set<String>> dbIdToGenes) {
        tablePane.setDBIDToGenes(dbIdToGenes);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
}
