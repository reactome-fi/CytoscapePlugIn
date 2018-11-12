package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanelName;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.drug.DrugDataSource;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This class is used to perform a drug impact pathway analysis via RESTful API.
 * @author wug
 *
 */
public class DrugPathwayImpactAnalysisAction implements ActionListener {
    private EventTreePane eventPane;

    public DrugPathwayImpactAnalysisAction() {
    }
    
    public void setEventPane(EventTreePane pane) {
        this.eventPane = pane;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String drug = actionEvent.getActionCommand(); // This is a hack. Drug is used here.
        // Just in case
        if (drug == null)
            return;
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setMinimum(0);
            progressPane.setMaximum(100);
            progressPane.setIndeterminate(true);
            progressPane.setTitle("Drug Impact Analysis");
            progressPane.setVisible(true);
            progressPane.setText("Run impact analysis...");
            RESTFulFIService restfulService = new RESTFulFIService();
            String results = restfulService.runDrugImpactAnalysis(drug, DrugDataSource.Targetome.toString()).trim();
            frame.getGlassPane().setVisible(false);
            if (results.length() == 0) {
                JOptionPane.showMessageDialog(frame,
                                              "The selected drug doesn't have any target in Reactome pathways!",
                                              "No Target",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            DrugPathwayImpactResultPane resultPane = new DrugPathwayImpactResultPane(eventPane, 
                                                                                     drug);
            resultPane.setResults(drug, results);
            // Need to select it
            PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in drug pathway impact analysis:\n" + e.getMessage(),
                                          "Error in Impact Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
    }
    
    
}
