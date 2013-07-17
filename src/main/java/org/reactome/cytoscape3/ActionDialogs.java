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
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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

/**
 * This class sets up the GUIs for the various actions. Since all GUI creation
 * is handled through this class, a "context" parameter is used to determine
 * which GUI is to be implemented.
 * 
 * @author Eric T. Dawson
 * 
 */
public class ActionDialogs extends JDialog
{
    private boolean isOkClicked;

    private String reactome_help_url = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";
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

    private FileUtil fileUtil;

    private CySwingApplication desktopApp;

    public ActionDialogs(String context, CySwingApplication desktopApp,
            FileUtil fileUtil)
    {
        this.desktopApp = desktopApp;
        this.fileUtil = fileUtil;

        if (context.equals("GeneSetMutationAnalysis")
                || context.equals("UserGuide") || context.equals("Microarray")
                || context.equals("Hotnet"))
        {
            init(context);
        }
    }

    // Retrieves the file path from the JFileTextfield
    public File getSelectedFile()
    {
        String text = fileTF.getText().trim();
        return new File(text);
    }

    // Allows the user to select a file using Cytoscape's
    // built-in file utility.
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

    public void init(String context)
    {
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

        if (context.equals("GeneSetMutationAnalysis"))
        {
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
            mainPane.addTab("Gene Set / Mutation Analysis", gsmaPanel);
            mainPane.setSelectedComponent(gsmaPanel);
        }

        if (context.equals("UserGuide"))
        {
            mainPane.setSize(500, 500);
            // Create a scrollable editor pane for the online user guide
            try
            {
                // //java.net.URL reactome_help_url = new
                // File("offline_help_page.html").toURI().toURL();
                JEditorPane htmlPane = new JEditorPane();
                htmlPane.setPage(reactome_help_url);
                JScrollPane scroll = new JScrollPane(htmlPane);
                mainPane.addTab("User Guide", scroll);
                mainPane.setSelectedComponent(scroll);
            }
            catch (Throwable t)
            {
                System.out
                        .println("The user guide is not available."
                                + "\nPlease visit the wiki at:\n"
                                + "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin\n");
            }

        }
        if (context.equals("Microarray"))
        {
            JPanel maaPanel = new JPanel();
            mainPane.addTab("Microarray Analysis", maaPanel);
            mainPane.setSelectedComponent(maaPanel);
        }
        getContentPane().add(mainPane, BorderLayout.CENTER);

        if (context.equals("Hotnet"))
        {
            JPanel hnaPanel = new JPanel();
            mainPane.addTab("HotNet Mutation Analysis", hnaPanel);
            mainPane.setSelectedComponent(hnaPanel);
        }
    }

    public boolean isOkClicked()
    {
        return this.isOkClicked;
    }

    public int getSampleCutoffValue()
    {
        String text = sampleCutoffField.getText().trim();
        if (text.length() == 0) return 0;
        return Integer.parseInt(text);
    }

    public boolean useLinkers()
    {
        return this.useLinkerBox.isSelected();
    }

    public JCheckBox getUnlinkedGeneBox()
    {
        return this.showUnlinkedBox;
    }

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

    public boolean showFIAnnotationsBeFetched()
    {
        return fetchFIAnnotations.isSelected();
    }
}
