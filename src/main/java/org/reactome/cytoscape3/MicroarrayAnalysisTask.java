package org.reactome.cytoscape3;

import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cancer.CancerGeneExpressionCommon;


public class MicroarrayAnalysisTask extends AbstractTask
{
    private JTextField fileField;
    private Boolean isOkClicked;
    private JTextField mclTIF;
    private JCheckBox corBox;
    @Override
    public void run(TaskMonitor tm) throws Exception
    {
        try
        {
            tm.setProgress(.15);
            tm.setStatusMessage("Loading microarray file...");
            CancerGeneExpressionCommon arrayHelper = new CancerGeneExpressionCommon();
            String fileName = fileField.getText().trim();
            Map<String, Map<String, Double>> geneToSampleToValue = arrayHelper.loadGeneExp(fileName);
            FINetworkService networkService = PlugInScopeObjectManager.getManager().getNetworkService();
            Set<String> fis = networkService.queryAllFIs();
            tm.setProgress(.30);
            tm.setStatusMessage("Calculating correlations...");
            Set<String> fisWithCorrs = arrayHelper.calculateGeneExpCorrForFIs(geneToSampleToValue,
                    fis,
                    corBox.isSelected(), 
                    null);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null,
                    "Error in MCL clustering: " + e.getMessage(), 
                    "Error in MCL Clustering", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); 
        }
        
    }

}
