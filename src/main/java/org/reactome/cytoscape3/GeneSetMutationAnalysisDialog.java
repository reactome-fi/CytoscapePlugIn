/*
 * Created on Sep 18, 2013
 *
 */
package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.util.DialogControlPane;

/**
 * @author gwu
 *
 */
public class GeneSetMutationAnalysisDialog extends FIActionDialog {
    //Gene Set / Mutation Analysis parameters
    // FI parameters and file parameters
    private JTextField sampleCutoffField;
    private JCheckBox useLinkerBox;
    private JCheckBox showUnlinkedBox;
    private JCheckBox chooseHomoBox;
    private JCheckBox fetchFIAnnotations;
    private JLabel sampleCutoffLabel;
    private JLabel sampleCommentLabel;
    // These radio buttons are used for file format
    private JRadioButton geneSetBtn;
    private JRadioButton geneSampleBtn;
    private JRadioButton mafBtn;
    
    /**
     * @param actionType
     */
    public GeneSetMutationAnalysisDialog() {
    }
    
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPane,
                                      Font font) {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel gsmaPanel = new JPanel();
        gsmaPanel.setLayout(new BoxLayout(gsmaPanel, BoxLayout.Y_AXIS));
        gsmaPanel.add(versionPane);
        // Pane for file parameters
        JPanel loadPanel = new JPanel();
        Border titleBorder = BorderFactory.createTitledBorder(etchedBorder,
                "File Parameters", TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        loadPanel.setBorder(titleBorder);
        loadPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 4, 0, 0);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // DialogControlPane controls access to the OK/Cancel
        // buttons of the main dialog pane.
        DialogControlPane controlPane = new DialogControlPane();
        JButton okBtn = controlPane.getOKBtn();

        // File box/browse button presented to the user, linked to
        // the file chooser, followed by format selection.
        JLabel fileChooseLabel = new JLabel("Choose data file:");
        fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        createFileChooserGui(fileChooseLabel, fileTF, okBtn, browseButton,
                loadPanel, constraints);

        JLabel fileFormatLabel = new JLabel("Specify file format: ");
        geneSetBtn = new JRadioButton("Gene set");
        geneSetBtn.setSelected(true);
        geneSampleBtn = new JRadioButton("Gene/sample number pair");
        mafBtn = new JRadioButton("NCI MAF (Mutation Annotation File)");
        ButtonGroup formatGroup = new ButtonGroup();
        formatGroup.add(geneSetBtn);
        formatGroup.add(geneSampleBtn);
        formatGroup.add(mafBtn);
        ActionListener formatBtnListner = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (geneSetBtn.isSelected())
                {
                    sampleCutoffLabel.setEnabled(false);
                    sampleCutoffField.setEnabled(false);
                    chooseHomoBox.setEnabled(false);
                    sampleCommentLabel.setEnabled(false);
                }
                else if (geneSampleBtn.isSelected())
                {
                    sampleCutoffLabel.setEnabled(true);
                    sampleCutoffField.setEnabled(true);
                    chooseHomoBox.setEnabled(false);
                    sampleCommentLabel.setEnabled(true);
                }
                else if (mafBtn.isSelected())
                {
                    sampleCutoffField.setEnabled(true);
                    sampleCutoffLabel.setEnabled(true);
                    chooseHomoBox.setEnabled(true);
                    sampleCommentLabel.setEnabled(true);
                }
            }
        };
        geneSetBtn.addActionListener(formatBtnListner);
        geneSampleBtn.addActionListener(formatBtnListner);
        mafBtn.addActionListener(formatBtnListner);

        // Gridbag constraints are defined below.
        constraints.gridx = 0;
        constraints.gridy = 1;
        loadPanel.add(fileFormatLabel, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        loadPanel.add(geneSetBtn, constraints);
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        loadPanel.add(geneSampleBtn, constraints);
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        loadPanel.add(mafBtn, constraints);
        // Add a sample cutoff value
        constraints.insets = new Insets(4, 4, 4, 4);
        sampleCutoffLabel = new JLabel("Choose sample cutoff:");
        sampleCutoffField = new JFormattedTextField(new Integer(2));
        sampleCutoffField.setColumns(4);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        loadPanel.add(sampleCutoffLabel, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        loadPanel.add(sampleCutoffField, constraints);
        // Add a text annotation
        sampleCommentLabel = new JLabel();
        sampleCommentLabel
                .setText("* Genes altered in 2 or more samples will be chosen if '2' is entered.");
        Font font2 = sampleCutoffLabel.getFont();
        Font commentFont = font2
                .deriveFont(Font.ITALIC, font.getSize() - 1);
        sampleCommentLabel.setFont(commentFont);
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 4, 0, 4);
        loadPanel.add(sampleCommentLabel, constraints);
        // Provide a checkbox to see if the user would like to choose
        // homologs.
        chooseHomoBox = new JCheckBox(
                "Choose genes mutated at both alleles");
        constraints.gridy = 6;
        constraints.gridheight = 1;
        constraints.insets = new Insets(4, 4, 4, 4);
        loadPanel.add(chooseHomoBox, constraints);

        gsmaPanel.add(loadPanel);

        // FI Network Construction Parameter Panel
        JPanel constructPanel = new JPanel();
        constructPanel.setBorder(BorderFactory.createTitledBorder(
                etchedBorder, "FI Network Construction Parameters",
                TitledBorder.LEFT, TitledBorder.CENTER, font));
        constructPanel.setLayout(new GridBagLayout());
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.0d;
        fetchFIAnnotations = new JCheckBox(
                "Fetch FI annotations (WARNING: Slow!)");
        JLabel label = new JLabel("* Annotations may be fetched later.");
        label.setFont(font2);
        constructPanel.add(fetchFIAnnotations, constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 4, 0, 4);
        constructPanel.add(label, constraints);
        useLinkerBox = new JCheckBox("Use linker genes");
        // Controls the checkbox for linker genes
        useLinkerBox.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                if (useLinkerBox.isSelected())
                {
                    showUnlinkedBox.setEnabled(false);
                }
                else
                {
                    showUnlinkedBox.setEnabled(true);
                }
            }
        });
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 4, 4, 4);
        constructPanel.add(useLinkerBox, constraints);
        showUnlinkedBox = new JCheckBox("Show genes not linked to others");
        constraints.gridy = 3;
        constructPanel.add(showUnlinkedBox, constraints);
        gsmaPanel.add(constructPanel);
        getContentPane().add(gsmaPanel, BorderLayout.CENTER);

        okBtn.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                isOkClicked = true;
                dispose();
            }
        });
        JButton cancelBtn = controlPane.getCancelBtn();
        cancelBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                isOkClicked = false;
                dispose();
            }
        });
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        // The following controls are disabled by default
        // as they are only necessary for gene/sample pair and MAF files.
        sampleCutoffLabel.setEnabled(false);
        sampleCutoffField.setEnabled(false);
        chooseHomoBox.setEnabled(false);
        sampleCommentLabel.setEnabled(false);
        
        
        return gsmaPanel;
    }
    
    @Override
    protected String getTabTitle() {
        return "Gene Set/Mutation Analysis";
    }
    
    /**
     * 
     * @return the sample cutoff value from the Gene Set/Mutation Analysis dialog.
     */
    public int getSampleCutoffValue()
    {
        String text = sampleCutoffField.getText().trim();
        if (text.length() == 0) return 0;
        return Integer.parseInt(text);
    }
    /**
     * 
     * @return Whether or not to use linker genes from the FI database in the network.
     */
    public boolean useLinkers()
    {
        return this.useLinkerBox.isSelected();
    }
    /**
     * 
     * @return Whether or not to use genes which are not linked to others in the final network.
     */
    public JCheckBox getUnlinkedGeneBox()
    {
        return this.showUnlinkedBox;
    }

    /**
     * 
     * @return Whether or not to use genes mutated in both alleles.
     */
    public boolean chooseHomoGenes()
    {
        return this.chooseHomoBox.isSelected();
    }

    public String getFileFormat()
    {
        if (mafBtn.isSelected()) return "MAF";
        if (geneSampleBtn.isSelected()) return "GeneSample";
        return "GeneSet";
    }

    public boolean shouldFIAnnotationsBeFetched()
    {
        return fetchFIAnnotations.isSelected();
    }
    
}
