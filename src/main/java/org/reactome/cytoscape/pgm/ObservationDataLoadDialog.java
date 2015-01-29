/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.util.swing.FileChooserFilter;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * This customized JDialog is used to load observation data for a factor graph.
 * @author gwu
 *
 */
public class ObservationDataLoadDialog extends FIActionDialog {
    private ObservationDataLoadPanel dataPanel;
    
    /**
     * Constructor.
     */
    public ObservationDataLoadDialog() {
    }
    
    @Override
    protected Collection<FileChooserFilter> createFileFilters() {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
        FileChooserFilter textFilter = new FileChooserFilter("Observation File",
                                                             "txt");
        filters.add(textFilter);
        return filters;
    }

    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPanel,
                                      Font font) {
        // There is no need for a FI version
        versionPanel.setVisible(false);
        
        dataPanel = new ObservationDataLoadPanel(font) {
            
            @Override
            protected void createFileChooserGui(JTextField fileTF,
                                                JLabel fileChooseLabel,
                                                JButton browseButton, JPanel loadPanel,
                                                GridBagConstraints constraints) {
                ObservationDataLoadDialog.this.createFileChooserGui(fileTF,
                                                                    fileChooseLabel,
                                                                    browseButton,
                                                                    loadPanel,
                                                                    constraints);
            }
        };
        return dataPanel;
    }
    
    public File getDNAFile() {
        return dataPanel.getDNAFile();
    }
    
    public File getGeneExpFile() {
        return dataPanel.getGeneExpFile();
    }
    
    public double[] getDNAThresholdValues() {
        return dataPanel.getDNAThresholdValues();
    }
    
    public double[] getGeneExpThresholdValues() {
        return dataPanel.getGeneExpThresholdValues();
    }
    
    public boolean isTwoCaseAnalysisSelected() {
        return dataPanel.isTwoCasesAnalysisSelected();
    }
    
    public File getTwoCasesSampleInfoFile() {
        return dataPanel.getTwoCasesSampleInfoFile();
    }
    
    /**
     * Calling this method will return nothing.
     */
    @Override
    public File getSelectedFile() {
        return null;
    }

    @Override
    protected String getTabTitle() {
        return "Load Observation Data";
    }

    @Override
    protected void doOKAction() {
        if (!dataPanel.validateValues())
            return;
        super.doOKAction();
    }
    
}
