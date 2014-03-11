/*
 * Created on Sep 18, 2013
 *
 */
package org.reactome.cytoscape3;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

/**
 * @author gwu
 *
 */
public class MicroArrayAnalysisDialog extends FIActionDialog {
    //Microarray Analysis
    private JTextField mclITF;
    private JCheckBox corBox;
    
    /**
     * Default constructor
     */
    public MicroArrayAnalysisDialog() {
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape3.FIActionDialog#createInnerPanel(org.reactome.cytoscape3.FIVersionSelectionPanel, java.awt.Font)
     */
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPane,
                                      Font font) {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel maaPanel = new JPanel();
        maaPanel.setLayout(new BoxLayout(maaPanel, BoxLayout.Y_AXIS));
        maaPanel.add(versionPane);
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
        
        JLabel fileChooseLabel = new JLabel("Choose Data File:");
        JButton browseButton = new JButton("Browse");
        fileTF = new JTextField();
        createFileChooserGui(fileTF,
                             fileChooseLabel,
                             browseButton,
                             filePanel, 
                             constraints);
        // Add a note text
        JTextArea noteTA = new JTextArea();
        Font font3 = filePanel.getFont();
        noteTA.setFont(font3.deriveFont(font3.getSize2D() - 2.0f));
        noteTA.setEditable(false);
        noteTA.setLineWrap(true);
        noteTA.setWrapStyleWord(true);
        noteTA.setBackground(filePanel.getBackground());
        noteTA.setText("Note: The array file should be a tab-delimited text file" +
                " with table header. The first column should be gene names. " +
                "All other columns should be expression values in samples. " +
                "One column for one sample. All values should be pre-normalized.");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        filePanel.add(noteTA, constraints);
        maaPanel.add(filePanel);
        
        // Set up a panel for correlation method
        JPanel correlationPanel = new JPanel();
        Border correlationBorder = BorderFactory.createTitledBorder(etchedBorder,
                "Correlation", TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        correlationPanel.setBorder(correlationBorder);
        correlationPanel.setLayout(layout);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        // Currently only Pearson correlation is supported
        JLabel corLabel = new JLabel("Correlation calculation method: ");
        JTextField corField = new JTextField("Pearson correlation");
        corField.setEditable(false);
        correlationPanel.add(corLabel, constraints);
        constraints.gridx = 1;
        correlationPanel.add(corField, constraints);
        corBox = new JCheckBox("Use absolute value (checked is preferred)");
        corBox.setSelected(true); // Default should be selected
        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        correlationPanel.add(corBox, constraints);
        maaPanel.add(correlationPanel);
        
        // Set up a panel for network clustering for weighted network
        JPanel clusterPanel = new JPanel();
        Border clusterBorder = BorderFactory.createTitledBorder(etchedBorder,
                "Network Clustering", TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        clusterPanel.setBorder(clusterBorder);
        clusterPanel.setLayout(layout);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel clusterLabel = new JLabel("Network clustering algorithm: ");
        clusterPanel.add(clusterLabel, constraints);
        JTextField clusterTF = new JTextField("MCL (Markov Cluster Algorithm)");
        clusterTF.setEditable(false);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        clusterPanel.add(clusterTF, constraints);
        JLabel paraLabel = new JLabel("Set inflation parameter (-I) for MCL: ");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        clusterPanel.add(paraLabel, constraints);
        mclITF = new JTextField("5.0");
        constraints.gridx = 1;
        clusterPanel.add(mclITF, constraints);
        JLabel mclIRangeLabel = new JLabel("1.2 - 5.0 (default 5.0)");
        constraints.gridx = 2;
        clusterPanel.add(mclIRangeLabel, constraints);
        maaPanel.add(clusterPanel);
        
        return maaPanel;
    }
    
    protected void doOKAction() {
        if (!validateFile(fileTF, fileTF))
            return;
        super.doOKAction();
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape3.FIActionDialog#getTabTitle()
     */
    @Override
    protected String getTabTitle() {
        return "Microarray Data Analysis";
    }
    
    /**
     * Verifies whether the file entered in the text field is a valid file.
     * @param fileTF
     * @param parentComp
     * @return
     */
    private boolean validateFile(JTextField fileTF,
                                 Component parentComp) {
        if (fileTF.getText().trim().length() == 0)
        {
            JOptionPane.showMessageDialog(parentComp,
                    "Please enter a file name in the file field",
                    "No File Name", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String text = fileTF.getText().trim();
        File file = new File(text);
        if (!file.exists())
        {
            JOptionPane
                    .showMessageDialog(
                            parentComp,
                            "The file you entered does not exist. Please enter a valid file name",
                            "Incorrect File Name", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    public boolean shouldAbsCorUsed() {
        return corBox.isSelected();
    }
    
    public double getInflation() {
        String text = mclITF.getText().trim();
        if (text.length() == 0)
            return 5.0d; // Default
        return new Double(text);
    }
}
