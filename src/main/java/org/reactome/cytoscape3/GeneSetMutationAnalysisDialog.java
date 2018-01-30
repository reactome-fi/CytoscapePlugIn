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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;

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
    // For pasting a set of genes
    private JButton enterGeneBtn;
    // Manually entered genes
    private String enteredGenes;
    
    /**
     * @param actionType
     */
    public GeneSetMutationAnalysisDialog() {
        setSize(500, 565);
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
                                                              "Gene Set Parameters", 
                                                              TitledBorder.LEFT, 
                                                              TitledBorder.CENTER,
                                                              font);
        loadPanel.setBorder(titleBorder);
        loadPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 4, 0, 0);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // File box/browse button presented to the user, linked to
        // the file chooser, followed by format selection.
        JLabel fileChooseLabel = new JLabel("Choose data file:");
        fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        constraints.gridy = 0;
        createFileChooserGui(fileTF,
                             fileChooseLabel, 
                             browseButton,
                             loadPanel,
                             constraints);
        
        createPasteGeneSetGui(loadPanel, constraints);
        
        JLabel fileFormatLabel = new JLabel("Specify format: ");
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
        constraints.gridy ++;
        loadPanel.add(fileFormatLabel, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        loadPanel.add(geneSetBtn, constraints);
        constraints.gridy ++;
        constraints.gridwidth = 1;
        loadPanel.add(geneSampleBtn, constraints);
        constraints.gridy ++;
        constraints.gridwidth = 2;
        loadPanel.add(mafBtn, constraints);
        // Add a sample cutoff value
        constraints.insets = new Insets(4, 4, 4, 4);
        sampleCutoffLabel = new JLabel("Choose sample cutoff:");
        sampleCutoffField = new JFormattedTextField(new Integer(2));
        sampleCutoffField.setColumns(4);
        constraints.gridx = 0;
        constraints.gridy ++;
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
        constraints.gridy ++;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 4, 0, 4);
        loadPanel.add(sampleCommentLabel, constraints);
        // Provide a checkbox to see if the user would like to choose
        // homologs.
        chooseHomoBox = new JCheckBox(
                "Choose genes mutated at both alleles");
        constraints.gridy ++;
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
        fetchFIAnnotations = new JCheckBox("Fetch FI annotations");
        JLabel label = new JLabel("* Annotations may be fetched later.");
        label.setFont(font2.deriveFont(Font.ITALIC, font2.getSize2D() - 1.0f));
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

        // as they are only necessary for gene/sample pair and MAF files.
        sampleCutoffLabel.setEnabled(false);
        sampleCutoffField.setEnabled(false);
        chooseHomoBox.setEnabled(false);
        sampleCommentLabel.setEnabled(false);
        return gsmaPanel;
    }

    private void createPasteGeneSetGui(JPanel loadPanel,
                                       GridBagConstraints constraints) {
        JLabel genesetPasteLabel = new JLabel("Or enter gene set:");
        enterGeneBtn = new JButton("Enter");
        constraints.gridx = 0;
        constraints.gridy ++;
        loadPanel.add(genesetPasteLabel, constraints);
        constraints.gridx = 1;
        loadPanel.add(enterGeneBtn, constraints);
        
        enterGeneBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                enterGenes();
            }
        });
    }
    
    /**
     * Paste a set of genes.
     */
    private void enterGenes() {
        GeneSetEnterDialog dialog = new GeneSetEnterDialog();
        if (enteredGenes != null) 
            dialog.geneTA.setText(enteredGenes);
        dialog.controlPane.getOKBtn().setEnabled(false); // Have to reset it as disabled in case the original text is placed.
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked)
            return;
        String text = dialog.geneTA.getText().trim();
        if (text.length() == 0) {
            enterGeneBtn.setText("Enter");
            enteredGenes = null;
        }
        else {
            // Display how many genes entered
            String[] tokens = text.split("\n");
            enterGeneBtn.setText(tokens.length + " Genes Entered");
            enteredGenes = text;
            fileTF.setText(null); // Remove all text
            SwingUtilities.invokeLater(new Runnable() {
                
                @Override
                public void run() {
                    okBtn.setEnabled(true); // The actual OK button of the whole dialog
                    geneSetBtn.setSelected(true); // Only Gene set is supported
                    geneSampleBtn.setEnabled(false);
                    mafBtn.setEnabled(false);
                }
            });
        }
    }
    
    /**
     * A String object contained genes manually entered.
     * @return
     */
    public String getEnteredGenes() {
        return enteredGenes;
    }
    
    public void setEnteredGenes(String genes) {
        this.enteredGenes = genes;
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
    
    /**
     * Customized JDialog to paste a set of genes.
     * @author gwu
     *
     */
    private class GeneSetEnterDialog extends JDialog {
        private boolean isOKClicked = false;
        private DialogControlPane controlPane;
        private JTextArea geneTA;
        
        public GeneSetEnterDialog() {
            super(GeneSetMutationAnalysisDialog.this);
            init();
        }
        
        private void init() {
            setTitle("Gene Set Input");
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), 
                                                                     BorderFactory.createEtchedBorder()));
            contentPane.setLayout(new BorderLayout());
            JLabel label = new JLabel("Enter or paste genes below (one line per gene):");
            contentPane.add(label, BorderLayout.NORTH);
            geneTA = new JTextArea();
            contentPane.add(new JScrollPane(geneTA), BorderLayout.CENTER);
            controlPane = new DialogControlPane();
            contentPane.add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    isOKClicked = true;
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    isOKClicked = false;
                }
            });
            controlPane.getOKBtn().setEnabled(false);
            
            synchronizeGUIs();
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            setLocationRelativeTo(getOwner());
            setSize(360, 325);
        }
        
        /**
         * Make sure all GUIs are in a consistent states.
         */
        private void synchronizeGUIs() {
            geneTA.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    controlPane.getOKBtn().setEnabled(true);
                    controlPane.getOKBtn().setEnabled(true);
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    controlPane.getOKBtn().setEnabled(true);
                    controlPane.getOKBtn().setEnabled(true);
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
            
            geneTA.addMouseListener(new MouseAdapter() {
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger())
                        doGeneTAPopup(e.getPoint());
                }
                
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger())
                        doGeneTAPopup(e.getPoint());
                }
                
            });
            
            // Related to the container GUIs. Adding these actions here makes
            // this dialog tied with this class only.
            fileTF.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    if (fileTF.getText().trim().length() > 0) {
                        resetEnterGeneGUIs();
                    }
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if (fileTF.getText().trim().length() > 0) {
                        resetEnterGeneGUIs();
                    }
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
            
        }
        
        private void resetEnterGeneGUIs() {
            enterGeneBtn.setText("Enter");
            enteredGenes = null;
            geneSampleBtn.setEnabled(true);
            mafBtn.setEnabled(true);
        }
        
        private void doGeneTAPopup(Point position) {
            JPopupMenu popup = new JPopupMenu();
            Action paste = new AbstractAction("Paste") {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    geneTA.paste();
                }
            };
            popup.add(paste);
            Action selectAll = new AbstractAction("Select All") {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    geneTA.selectAll();
                }
            };
            popup.add(selectAll);
            popup.show(geneTA, position.x, position.y);
        }
       
    }
    
}
