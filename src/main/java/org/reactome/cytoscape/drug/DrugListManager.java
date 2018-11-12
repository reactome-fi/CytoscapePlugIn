/*
 * Created on Jan 22, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.gk.util.ProgressPane;
import org.jdom.Element;
import org.reactome.cytoscape.service.AbstractPathwayEnrichmentAnalysisTask;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.dataModel.Drug;
import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * This class is used to manage the list of cancer drugs fetched from the server-side application.
 * @author gwu
 *
 */
public class DrugListManager {
    private static DrugListManager manager;
    private Map<DrugDataSource, List<Drug>> sourceToDrugs;
    private AbstractPathwayEnrichmentAnalysisTask enrichmentTask;
    private ActionListener runImpactAnalysisAction;
    
    /**
     * Default constructor. This is a private method so that only one copy of DrugListManager
     * can be used in the application.
     */
    private DrugListManager() {
        sourceToDrugs = new HashMap<>();
    }
    
    public static DrugListManager getManager() {
        if (manager == null)
            manager = new DrugListManager();
        return manager;
    }
    
    public ActionListener getRunImpactAnalysisAction() {
        return runImpactAnalysisAction;
    }

    public void setRunImpactAnalysisAction(ActionListener runImpactAnalysisAction) {
        this.runImpactAnalysisAction = runImpactAnalysisAction;
    }

    public void setEnrichmentTask(AbstractPathwayEnrichmentAnalysisTask task) {
        this.enrichmentTask = task;
    }
    
    public void overlayTargetsToPathways(Set<String> targets) {
        if (enrichmentTask == null)
            return;
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        StringBuilder builder = new StringBuilder();
        for (String target : targets)
            builder.append(target).append("\n");
        builder.deleteCharAt(builder.length() - 1);
        enrichmentTask.setGeneList(builder.toString());
        taskManager.execute(new TaskIterator(enrichmentTask));
    }
    
    public void showDrugTargetInteractions(List<String> drugNames, DrugListView listView) {
        if (drugNames == null || drugNames.size() == 0)
            return;
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            setProgressPane(frame, "Fetch Drug Targets");
            RESTFulFIService restfulService = new RESTFulFIService();
            Element rootElm = restfulService.queryInteractionsForDrugs(drugNames, 
                                                                       listView.getDataSource() + "");
            DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
            parser.parse(rootElm);
            List<Interaction> interactions = parser.getInteractions();
            frame.getGlassPane().setVisible(false);
            showInteractions(interactions, listView);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in querying drug targers:\n" + e.getMessage(),
                                          "Error in Querying Drugs",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
    }
    
    private void showInteractions(List<Interaction> interactions, JDialog parent) {
        if (interactions == null || interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No interactions to display.",
                                          "Empty Interactions",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        InteractionListView view = new InteractionListView(parent);
        view.setInteractions(interactions);
        view.setModal(true);
        view.setVisible(true);
    }
    
    public void listDrugs(DrugDataSource dataSource) {
        List<Drug> drugs = sourceToDrugs.get(dataSource);
        if (drugs != null && drugs.size() > 0) {
            showDrugs(drugs, dataSource);
            return;
        }
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            setProgressPane(frame,
                            "Fetch Drugs");
            RESTFulFIService restfulService = new RESTFulFIService();
            Element rootElm = restfulService.listDrugs(dataSource.toString());
            DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
            parser.parse(rootElm);
            Map<String, Drug> idToDrug = parser.getIdToDrug();
            drugs = new ArrayList<Drug>(idToDrug.values());
            sourceToDrugs.put(dataSource, drugs);
            frame.getGlassPane().setVisible(false);
            showDrugs(drugs, dataSource);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in querying cancer drugs:\n" + e.getMessage(),
                                          "Error in Querying Drugs",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
    }

    private void setProgressPane(JFrame frame,
                                 String title) {
        ProgressPane progressPane = new ProgressPane();
        frame.setGlassPane(progressPane);
        progressPane.setMinimum(0);
        progressPane.setMaximum(100);
        progressPane.setIndeterminate(true);
        progressPane.setTitle(title);
        progressPane.setVisible(true);
        progressPane.setText("Querying the server...");
    }
    
    private void showDrugs(List<Drug> drugs, DrugDataSource dataSource) {
        if (drugs == null || drugs.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot fetch any drug from the database.",
                                          "No Drug",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        DrugListView listView = null;
        if (dataSource == DrugDataSource.Targetome)
            listView = new DrugListView();
        else if (dataSource == DrugDataSource.DrugCentral)
            listView = new DrugCentralDrugListView();
        listView.setDataSource(dataSource);
        listView.setDrugs(drugs);
        listView.setModal(true);
        listView.setVisible(true);
    }
}
