/*
 * Created on Sep 18, 2013
 *
 */
package org.reactome.cytoscape3;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * A customized JDialog for HotnetAnalysis action.
 * @author gwu
 *
 */
public class HotNetAnalysisDialog extends FIActionDialog {
    
    private JTextField deltaTF;
    private JCheckBox deltaAutoBox;
    private JTextField fdrCutoffTF;
    private JTextField permutationTF;
    private JLabel fdrLabel;
    
    /**
     * Default constructor.
     */
    public HotNetAnalysisDialog() {
    }
    
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape3.FIActionDialog#createInnerPanel(org.reactome.cytoscape3.FIVersionSelectionPanel, java.awt.Font)
     */
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPane,
                                      Font font) {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel hnaPanel = new JPanel();
        //Select the Box Layout Manager and add the FI Version
        //Selection panel to the tabbed pane.
        hnaPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        hnaPanel.setLayout(new BoxLayout(hnaPanel, BoxLayout.Y_AXIS));
        hnaPanel.add(versionPane);
        
        //File parameter panel
        JPanel filePanel = new JPanel();
        Border fileBorder = BorderFactory.createTitledBorder(etchedBorder,
                "File Parameters", TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        filePanel.setBorder(fileBorder);
        GridBagLayout layout = new GridBagLayout();
        filePanel.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.1;
        JLabel fileChooseLabel = new JLabel("Choose data file:");
        fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        createFileChooserGui(fileTF,
                             fileChooseLabel, 
                             browseButton,
                             filePanel,
                             constraints);
        JLabel fileFormatLabel = new JLabel("Specify file format:");
        constraints.gridy = 1;
        constraints.gridx = 0;
        filePanel.add(fileFormatLabel, constraints);
        final JRadioButton mafBtn = new JRadioButton("NCI MAF (Mutation Annotation File)");
        mafBtn.setSelected(true);
        ButtonGroup formatGroup = new ButtonGroup();
        formatGroup.add(mafBtn); 
        constraints.gridy = 1;
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        filePanel.add(mafBtn, constraints);
        
        hnaPanel.add(filePanel);
        
        //Panel for delta, permutations, and other
        //Analysis parameters
        JPanel paramPane = new JPanel();
        Border paramBorder = BorderFactory.createTitledBorder(etchedBorder,
                "Analysis Parameters", TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        paramPane.setBorder(paramBorder);
        paramPane.setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel deltaLabel = new JLabel("Delta:");
        paramPane.add(deltaLabel, constraints);
        deltaTF = new JTextField();
        constraints.gridx = 1;
        constraints.gridwidth = 3;
        paramPane.add(deltaTF, constraints);
        deltaAutoBox = new JCheckBox("Auto");
        deltaAutoBox.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                fdrLabel.setEnabled(deltaAutoBox.isSelected());
                fdrCutoffTF.setEnabled(deltaAutoBox.isSelected());
                deltaTF.setEnabled(!deltaAutoBox.isSelected());
            }
        });
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 4, 0, 4);
        paramPane.add(deltaAutoBox, constraints);
        fdrLabel = new JLabel("FDR Cutoff:");
        fdrLabel.setEnabled(false);
        constraints.gridx = 2;
        paramPane.add(fdrLabel, constraints);
        fdrCutoffTF = new JTextField();
        fdrCutoffTF.setText("0.25"); // Use 0.25 as default for delta auto-selection
        fdrCutoffTF.setEnabled(false);
        fdrCutoffTF.setColumns(5);
        constraints.gridx = 3;
        paramPane.add(fdrCutoffTF, constraints);
        // Add a warning
        JLabel autoLabel = new JLabel("*Use of the \"Auto\" option may drastically increase run time.");
        Font font2 = autoLabel.getFont();
        font2 = font.deriveFont(Font.ITALIC);
        autoLabel.setFont(font2);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 4;
        paramPane.add(autoLabel, constraints);

        JLabel permutationLabel = new JLabel("Number of permutations:");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(4, 4, 4, 4);
        paramPane.add(permutationLabel, constraints);
        permutationTF = new JTextField(100);
        permutationTF.setText("100"); // Use 100 as the default for permutation test
        constraints.gridx = 1;
        constraints.gridwidth = 3;
        paramPane.add(permutationTF, constraints);
        hnaPanel.add(paramPane);
        return hnaPanel;
    }
    
    protected void doOKAction() {
        if (!validateParameters())
            return;
        super.doOKAction();
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape3.FIActionDialog#getTabTitle()
     */
    @Override
    protected String getTabTitle() {
        return "HotNet Mutation Analysis";
    }
    
    /**
     * Checks whether the number of permutations is within the permissible range (<1000).
     * @return
     */
    private boolean validateParameters() {
        // Make sure permutation number no more than 1000
        String text = permutationTF.getText().trim();
        if (text.length() == 0 || !text.matches("(\\d)+") || Integer.parseInt(text) > 1000) {
            JOptionPane.showMessageDialog(this,
                                          "Permutation number can not be empty. It must be an integer " +
                                          "and not bigger than 1000. The default is 100.",
                                          "Invalid Permutation Number", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    public boolean isAutoDeltaSelected() {
        return this.deltaAutoBox.isSelected();
    }
    
    public Double getDelta() {
        return new Double(deltaTF.getText().trim());
    }
    
    public Integer getPermutationNumber() {
        return new Integer(permutationTF.getText().trim());
    }
    
    public Double getFDRCutoff() {
        return new Double(fdrCutoffTF.getText().trim());
    }
    

    
}
