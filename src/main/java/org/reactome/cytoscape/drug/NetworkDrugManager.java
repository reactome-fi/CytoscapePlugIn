/*
 * Created on Dec 29, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.jdom.Element;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.Interaction;

/**
 * This manager is used to handle cancer drug overlay for the FI network view.
 * @author gwu
 *
 */
public class NetworkDrugManager extends DrugTargetInteractionManager {
    private static NetworkDrugManager manager;
    private Map<String, List<Interaction>> geneToInteractions;
    
    /**
     * Default constructor.
     */
    protected NetworkDrugManager() {
        geneToInteractions = new HashMap<>();
    }
    
    public static NetworkDrugManager getManager() {
        if (manager == null)
            manager = new NetworkDrugManager();
        return manager;
    }
    
    public void fetchCancerDrugs(CyNetworkView view) throws Exception {
        List<CyNode> nodeList = view.getModel().getNodeList(); 
        if (nodeList == null || nodeList.size() == 0)
            return;
        Set<String> genes = new HashSet<String>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : nodeList) {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            genes.add(nodeName);
        }
        if (genes.size() == 0)
            return; // Nothing to be displayed
        RESTFulFIService service = new RESTFulFIService();
        Element element = service.queryDrugTargetInteractionsForGenes(genes);
        DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
        parser.parse(element);
        List<Interaction> interactions = parser.getInteractions();
        if (interactions == null || interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No cancer drugs can be found for genes displayed in the network.",
                                          "No Cancer Drugs",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Need to sort these interactions
        cacheInteractions(genes, interactions);
        // Display these interactions as edges, one of which may be supported by multiple interactions.
        displayInteractions(genes, view);
    }
    
    private void displayInteractions(Set<String> genes,
                                     CyNetworkView view) {
        FINetworkGenerator helper = new FINetworkGenerator();
        for (String gene : genes) {
            List<Interaction> interactions = geneToInteractions.get(gene);
            if (interactions.size() == 0)
                continue;
            // Get a set of drugs
            Set<String> drugs = new HashSet<>();
            for (Interaction interaction : interactions) {
                drugs.add(interaction.getIntDrug().getDrugName());
            }
            helper.addFIPartners(gene,
                                 drugs,
                                 "Drug",
                                 false,
                                 view);
        }
    }   
    
    private void cacheInteractions(Set<String> genes,
                                   List<Interaction> interactions) {
        for (Interaction interaction : interactions) {
            String target = interaction.getIntTarget().getTargetName();
            List<Interaction> targetInts = geneToInteractions.get(target);
            if (targetInts == null) {
                targetInts = new ArrayList<>();
                geneToInteractions.put(target, targetInts);
            }
            targetInts.add(interaction);
        }
        // Keep track empty interactions
        for (String gene : genes) {
            List<Interaction> geneInts = geneToInteractions.get(gene);
            if (geneInts == null) {
                geneInts = new ArrayList<>();
                geneToInteractions.put(gene, geneInts);
            }
        }
    }
    
    public void filterCancerDrugs(CyNetworkView networkView) {
        
    }
    
    public void removeCancerDrugs(CyNetworkView networkView) {
        
    }
}
