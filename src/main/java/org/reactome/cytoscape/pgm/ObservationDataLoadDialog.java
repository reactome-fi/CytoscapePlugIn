/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.util.swing.FileChooserFilter;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * This customized JDialog is used to load observation data for a factor graph.
 * @author gwu
 *
 */
public class ObservationDataLoadDialog extends FIActionDialog {
    private List<JTextField> dnaTFs;
    private List<JTextField> geneExpTFs;
    
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
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        
        dnaTFs = new ArrayList<JTextField>();
        JPanel dnaPane = createDataPane("CNV", 
                                        dnaTFs,
                                        new double[]{-0.95, 0.95});
        Border titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                              "DNA Data",
                                                              TitledBorder.LEFT,
                                                              TitledBorder.CENTER,
                                                              font);
        dnaPane.setBorder(titleBorder);
        contentPane.add(dnaPane);
        
        geneExpTFs = new ArrayList<JTextField>();
        JPanel geneExpressionPane = createDataPane("gene expression", 
                                                   geneExpTFs,
                                                   new double[]{-1.64, 1.64});
        titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                       "Gene Expression",
                                                       TitledBorder.LEFT,
                                                       TitledBorder.CENTER,
                                                       font);
        geneExpressionPane.setBorder(titleBorder);
        contentPane.add(geneExpressionPane);

        return contentPane;
    }
    
    private JPanel createDataPane(String dataType,
                                  List<JTextField> tfs,
                                  double[] defaultValues) {
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5d;
        
        JLabel label = new JLabel("Choose " + dataType + " file: ");
        JTextField fileTF = new JTextField();
        JButton browseBtn = new JButton("Browse");
        createFileChooserGui(fileTF, label, browseBtn, pane, constraints);
        
        label = new JLabel("Choose threshold values for discretizing:");
        constraints.gridwidth = 3;
        constraints.gridx = 0;
        constraints.gridy ++;
        pane.add(label, constraints);
        
        constraints.gridwidth = 1;
        JTextField state0TF = addStateSection("State 0: less than ", pane, constraints);
        final JTextField state1TF = addStateSection("State 1: less than ", pane, constraints);
        final JTextField state2TF = addStateSection("State 2: no less than ", pane, constraints);
        state2TF.setEditable(false);
        state1TF.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                state2TF.setText(state1TF.getText());
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                state2TF.setText(state1TF.getText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        
        tfs.add(fileTF);
        tfs.add(state0TF);
        tfs.add(state1TF);
        tfs.add(state2TF);
        state0TF.setText(defaultValues[0] + "");
        state1TF.setText(defaultValues[1] + "");

        return pane;
    }
    
    private JTextField addStateSection(String labelText,
                                 JPanel pane, 
                                 GridBagConstraints constraints) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        constraints.gridy ++;
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        pane.add(label, constraints);
        JTextField stateTF = new JTextField();
        stateTF.setColumns(2);
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add(stateTF, constraints);
        
        return stateTF;
    }
    
    public File getDNAFile() {
        return getFile(dnaTFs);
    }
    
    private File getFile(List<JTextField> tfs) {
        String text = tfs.get(0).getText().trim();
        if (text.length() == 0)
            return null;
        File file = new File(text);
        if (file.exists())
            return file;
        return null;
    }
    
    public File getGeneExpFile() {
        return getFile(geneExpTFs);
    }
    
    private boolean validateDNAParameters() {
        if (getDNAFile() == null)
            return true; // Nothing is needed
        return validateThresholdValues(dnaTFs);
    }
    
    private boolean validateGeneExpParameters() {
        if (getGeneExpFile() == null)
            return true;
        return validateThresholdValues(geneExpTFs);
    }
    
    private boolean validateThresholdValues(List<JTextField> tfs) {
        // Make sure numbers are entered
        String text1 = tfs.get(1).getText().trim();
        String text2 = tfs.get(2).getText().trim();
        if (text1.length() == 0 || text2.length() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "Please enter threshold values.",
                                          "No Threshold Values",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            double value1 = new Double(text1);
            double value2 = new Double(text2);
            if (value1 >= value2) {
                JOptionPane.showMessageDialog(this,
                                              "Please make sure the first threshold value is less than the second.",
                                              "Wrong Threshold Values",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                                          "Please make sure the entered threshold values are numeric.",
                                          "Wrong Threshold Values",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
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
        if (!validateDNAParameters() || !validateGeneExpParameters())
            return;
        super.doOKAction();
    }
    
}
