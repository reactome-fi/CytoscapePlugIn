package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.gk.util.DialogControlPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInUtilities;


/**
 * This class sets up the GUIs for the various actions. Since all GUI creation
 * is handled through this class, a "context" parameter is used to determine
 * which GUI is to be implemented. Think of this as a lame GUI factory.
 * 
 * @author Eric T. Dawson
 * 
 */
@SuppressWarnings("serial")
public class ActionDialogs extends JDialog
{

    private String reactome_help_url = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";
    //Gene Set / Mutation Analysis parameters
    // FI parameters and file parameters
    private JTextField sampleCutoffField;
    private JCheckBox useLinkerBox;
    private JCheckBox showUnlinkedBox;
    private JCheckBox chooseHomoBox;
    private JCheckBox fetchFIAnnotations;
    private JLabel sampleCutoffLabel;
    private JLabel sampleCommentLabel;
    private JTextField fileTF;
    // These radio buttons are used for file format
    private JRadioButton geneSetBtn;
    private JRadioButton geneSampleBtn;
    private JRadioButton mafBtn;

    //Microarray Analysis
    private JTextField mclITF;
    private JCheckBox corBox;
    
    //HotNet Analysis
    private JTextField deltaTF;
    private JCheckBox deltaAutoBox;
    private JTextField fdrCutoffTF;
    private JTextField permutationTF;
    private JLabel fdrLabel;
    
    //Universal parameters
    boolean isOkClicked;
    private FileUtil fileUtil;
    private CySwingApplication desktopApp;

    public ActionDialogs(String actionType)
    {
            init(actionType);
    }
    /**
     * Retrieves the file given from the path in the textbox.
     * @return The file at the specified location
     */
    public File getSelectedFile()
    {
        String text = fileTF.getText().trim();
        return new File(text);
    }

    /**
     * Allows a user to browse for a file using Cytoscape's built-in file utility.
     * @param tf
     */
    private void getFile(JTextField tf)
    {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();

        String[] mafExts = new String[2];
        mafExts[0] = "txt";
        mafExts[1] = "maf";
        FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files",
                mafExts);
        filters.add(mafFilter);

        File dataFile = fileUtil.getFile(desktopApp.getJFrame(),
                "Please select your file for analysis", FileUtil.LOAD, filters);
        if (dataFile == null) return;
        tf.setText(dataFile.getAbsolutePath());

    }

    /**
     * Sets up actions for the various buttons and file fields
     * and places them according to a given set of layout constraints.
     * @param fileChooseLabel
     * @param tf
     * @param okBtn
     * @param browseButton
     * @param loadPanel
     * @param constraints
     */
    protected void createFileChooserGui(final JLabel fileChooseLabel,
            final JTextField tf, final JButton okBtn,
            final JButton browseButton, JPanel loadPanel,
            GridBagConstraints constraints)
    {

        loadPanel.add(fileChooseLabel, constraints);
        fileTF.getDocument().addDocumentListener(new DocumentListener()
        {

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                if (tf.getText().trim().length() > 0)
                {
                    okBtn.setEnabled(true);
                }
                else
                {
                    okBtn.setEnabled(false);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                if (fileTF.getText().trim().length() > 0)
                {
                    okBtn.setEnabled(true);
                }
                else
                {
                    okBtn.setEnabled(false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        tf.setColumns(20);
        constraints.gridx = 1;
        constraints.weightx = 0.80;
        loadPanel.add(tf, constraints);
        browseButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getFile(tf);
            }
        });

        loadPanel.add(browseButton);
        // Disable okBtn as default
        okBtn.setEnabled(false);
    }

    /**
     * Creates the graphical user interfaces for each of the Reactome FI analyses.
     * @param actionType A string indicating the type of action to be performed (GeneSetMutationAnalysis, UserGuide, Microarray, Hotnet).
     */
    public void init(String actionType)
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference cyAppRef = context.getServiceReference(CySwingApplication.class.getName());
        ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
        if (cyAppRef != null && fileUtilRef != null)
        {
            CySwingApplication desktopApp = (CySwingApplication) context.getService(cyAppRef);
            this.desktopApp = desktopApp;
            FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);
            this.fileUtil = fileUtil;
        }
        // Main dialog pane. A tabbed pane is used
        // to mimic the Cytoscape GUI and provide room
        // for future tabbed user interfaces.
        setTitle("Reactome FI");
        JTabbedPane mainPane = new JTabbedPane();
        setSize(475, 535);
        Font font = new Font("Verdana", Font.BOLD, 13);

        // Pane for FI Network version selection.
        FIVersionSelectionPanel versionPane = new FIVersionSelectionPanel();
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border versionBorder = BorderFactory.createTitledBorder(etchedBorder,
                versionPane.getTitle(), TitledBorder.LEFT, TitledBorder.CENTER,
                font);
        versionPane.setBorder(versionBorder);
        JPanel innerPanel = null;
        String tabTitle = "";
        if (actionType.equals("GeneSetMutationAnalysis"))
        {
            innerPanel = makeGSMAPanel(versionPane);
            tabTitle = "Gene Set / Mutation Analysis";
        }
        else if (actionType.equals("Microarray"))
        {
            innerPanel = makeMicroarrayPanel(versionPane);
            tabTitle = "Microarray Analysis";
        }
        else if (actionType.equals("Hotnet"))
        {
            innerPanel = makeHotnetPanel(versionPane);
            tabTitle = "HotNet Analysis";
        }
       
       mainPane.addTab(tabTitle, innerPanel);
       mainPane.setSelectedComponent(innerPanel);
       add(mainPane);
       context.ungetService(fileUtilRef);
       context.ungetService(cyAppRef);
        
    }
    
    private JPanel makeHotnetPanel(FIVersionSelectionPanel versionPane)
    {
        Font font = new Font("Verdana", Font.BOLD, 13);
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
        DialogControlPane controlPane = new DialogControlPane();
        JButton okBtn = controlPane.getOKBtn();
        JLabel fileChooseLabel = new JLabel("Choose data file:");
        fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        createFileChooserGui(fileChooseLabel, fileTF, okBtn, browseButton, filePanel, constraints);
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
        okBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!validateParameters())
                    return;
                setVisible(false);
                dispose();
                isOkClicked = true;
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
//        okBtn.setEnabled(false); // If no file is selected
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        return hnaPanel;
    }
    
    private JPanel makeMicroarrayPanel(FIVersionSelectionPanel versionPane)
    {
        // TODO Auto-generated method stub
        Font font = new Font("Verdana", Font.BOLD, 13);
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
        
        // Add a control panel
        DialogControlPane controlPane = new DialogControlPane();
        JLabel fileChooseLabel = new JLabel("Choose Data File:");
        JButton okBtn = controlPane.getOKBtn();
        JButton browseButton = new JButton("Browse");
        fileTF = new JTextField();
        createFileChooserGui(fileChooseLabel, fileTF, okBtn, browseButton, filePanel, constraints);
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
        
        okBtn.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if (!validateFile(fileTF, desktopApp.getJFrame()))
                    return;
                setVisible(false);
                dispose();
                isOkClicked = true;
            }});
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        okBtn.setEnabled(false); // If no file is selected
        getContentPane().add(controlPane, BorderLayout.SOUTH);

        setLocationRelativeTo(getOwner());
        
        return maaPanel;
    }
    private JPanel makeGSMAPanel(FIVersionSelectionPanel versionPane)
    {
        Font font = new Font("Verdana", Font.BOLD, 13);
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
    /**
     * 
     * @return whether the ok button has been clicked or not.
     */
    public boolean isOkClicked()
    {
        return this.isOkClicked;
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
     * Verifies whether the file entered in the text field is a valid file.
     * @param fileTF
     * @param parentComp
     * @return
     */
    protected boolean validateFile(JTextField fileTF,
            java.awt.Component parentComp)
    {
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
    public boolean isSelectedCorBox()
    {
        return corBox.isSelected();
    }
    public String getMclTIFPath()
    {
        return mclITF.getText().trim();
    }
    
}
