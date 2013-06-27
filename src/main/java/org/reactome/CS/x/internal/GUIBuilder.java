package org.reactome.CS.x.internal;

import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.gk.util.DialogControlPane;


public class GUIBuilder extends JDialog
{
    //
    private boolean isOkClicked;

    private String reactome_help_url = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";
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



    
    public GUIBuilder(String context, FileUtil fileUtil)
    {
	this.fileUtil = fileUtil;

	if (context.equals("GSMA") || context.equals("UGA") || context.equals("MAA"))
		init(context);
	else
	    System.out.println("There is a bug. Please send word to the developers.");
    }
    public File getSelectedFile() {
        String text = fileTF.getText().trim();
        return new File(text);
    }
    protected void getFile(String format, Component filePanel){
	Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
	if (format.equals("MAF"))
	{
	    String [] mafExts = new String [3];
	    mafExts[0] = "txt"; mafExts[1] = "protected.maf"; mafExts[2] = "maf";
	    FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files", mafExts);
	    filters.add(mafFilter);
	}
	else if (format.equals("GeneSet"))
	{
	    String [] gsExts = new String [3];
	    gsExts[0] = "txt"; gsExts[1] = "protected.maf"; gsExts[2] = "maf";
	    FileChooserFilter mafFilter = new FileChooserFilter("Gene Set Files", gsExts);
	    filters.add(mafFilter);
	}
	else
	{
	    String [] mafExts = new String [3];
	    mafExts[0] = "txt"; mafExts[1] = "protected.maf"; mafExts[2] = "maf";
	    FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files", mafExts);
	    filters.add(mafFilter);
	}
	File dataFile = fileUtil.getFile(filePanel, "Please select your file for analysis", FileUtil.LOAD, filters);
    }
    public void init(String context)
    {
	setTitle("Reactome FI");
	JTabbedPane mainPane = new JTabbedPane();
	setSize(540, 535);
	
	//Pane for FI Network version selection.
//    	FIVersionSelectionPanel versionPane = new FIVersionSelectionPanel();
//            Border etchedBorder2 = BorderFactory.createEtchedBorder();
//            Border titleBorder = BorderFactory.createTitledBorder(etchedBorder2,
//                                                                  versionPane.getTitle());
//            versionPane.setBorder(titleBorder);

        if (context.equals("GSMA")){
            JPanel gsmaPanel = new JPanel();
    	Border etchedBorder = BorderFactory.createEtchedBorder();
    	gsmaPanel.setLayout(new BoxLayout(gsmaPanel, BoxLayout.Y_AXIS));
    	
    	
    	
//            gsmaPanel.add(versionPane);
    	//Add the Cytoscape-rolled FileUtil file chooser
    	
    	//Pane for file parameters
    	JPanel loadPanel = new JPanel();
            etchedBorder = BorderFactory.createEtchedBorder();
            Border titleBorder = BorderFactory.createTitledBorder(etchedBorder, "File Parameters");
            loadPanel.setBorder(titleBorder);
            loadPanel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 4, 0, 0);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
           // JLabel fileChooseLabel = new JLabel("Choose gene set/mutation file:");
            //fileTF = new JTextField();
            
            JLabel fileFormatLabel = new JLabel("Specify file format:");
            geneSetBtn = new JRadioButton("Gene set");
            geneSetBtn.setSelected(true);
            geneSampleBtn = new JRadioButton("Gene/sample number pair");
            mafBtn = new JRadioButton("NCI MAF (Mutation Annotation File)");
            ButtonGroup formatGroup = new ButtonGroup();
            formatGroup.add(geneSetBtn);
            formatGroup.add(geneSampleBtn);
            formatGroup.add(mafBtn);
            ActionListener formatBtnListner = new ActionListener() {
                @Override
		public void actionPerformed(ActionEvent e) {
                    if (geneSetBtn.isSelected()) {
                        sampleCutoffLabel.setEnabled(false);
                        sampleCutoffField.setEnabled(false);
                        chooseHomoBox.setEnabled(false);
                        sampleCommentLabel.setEnabled(false);
                    }
                    else if (geneSampleBtn.isSelected()) {
                        sampleCutoffLabel.setEnabled(true);
                        sampleCutoffField.setEnabled(true);
                        chooseHomoBox.setEnabled(false);
                        sampleCommentLabel.setEnabled(true);
                    }
                    else if (mafBtn.isSelected()) {
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
            sampleCommentLabel.setText("* Genes altered in 2 or more samples will be chosen if enter 2.");
            Font font = sampleCutoffLabel.getFont();
            Font commentFont = font.deriveFont(Font.ITALIC, font.getSize() - 1);
            sampleCommentLabel.setFont(commentFont);
            constraints.gridx = 0;
            constraints.gridy = 5;
            constraints.gridwidth = 3;
            constraints.insets = new Insets(0, 4, 0, 4);
            loadPanel.add(sampleCommentLabel, constraints);
            // To control homo or not
            chooseHomoBox = new JCheckBox("Choose genes mutated at both alleles");
            constraints.gridy = 6;
            constraints.gridheight = 1;
            constraints.insets = new Insets(4, 4, 4, 4);
            loadPanel.add(chooseHomoBox, constraints);
            
            gsmaPanel.add(loadPanel);
            
            //FI Network Construction Parameter Panel
            JPanel constructPanel = new JPanel();
            constructPanel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "FI Network Construction Parameters"));
            constructPanel.setLayout(new GridBagLayout());
            constraints.gridheight = 1;
            constraints.gridwidth = 1;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.weightx = 0.0d;
            fetchFIAnnotations = new JCheckBox("Fetch FI annotations (Slow step!)");
            JLabel label = new JLabel("* Annotations may be fetched later.");
            label.setFont(font);
            constructPanel.add(fetchFIAnnotations, constraints);
            constraints.gridy = 1;
            constraints.insets = new Insets(0, 4, 0, 4);
            constructPanel.add(label, constraints);
            useLinkerBox = new JCheckBox("Use linker genes");
            // To control another JCheckBox
            useLinkerBox.addChangeListener(new ChangeListener() {
                @Override
		public void stateChanged(ChangeEvent e) {
                    if (useLinkerBox.isSelected())
                        showUnlinkedBox.setEnabled(false);
                    else
                        showUnlinkedBox.setEnabled(true);
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

            DialogControlPane controlPane = new DialogControlPane();
            JButton okBtn = controlPane.getOKBtn();
            okBtn.addActionListener(new ActionListener() {
                
                @Override
		public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    dispose();
                }
            });
            JButton cancelBtn = controlPane.getCancelBtn();
            cancelBtn.addActionListener(new ActionListener() {
                @Override
		public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            okBtn.setDefaultCapable(true);
            getRootPane().setDefaultButton(okBtn);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            // Default: the following controls should be disabled!
            sampleCutoffLabel.setEnabled(false);
            sampleCutoffField.setEnabled(false);
            chooseHomoBox.setEnabled(false);
            sampleCommentLabel.setEnabled(false);
            mainPane.addTab("Gene Set / Mutant Analysis", gsmaPanel);
            mainPane.setSelectedComponent(gsmaPanel);
        }
        if (context.equals("UGA")){
            mainPane.setSize(500, 500);
          //Create a scrollable editor pane for the online user guide
//    	try{
//    		//java.net.URL reactome_help_url = new File("offline_help_page.html").toURI().toURL();
//    		JEditorPane htmlPane = new JEditorPane();
//    		htmlPane.setPage(reactome_help_url);
//    		JScrollPane scroll = new JScrollPane(htmlPane);
//    		mainPane.addTab("User Guide", scroll);
//    		}
//    		catch (Throwable t){
//    		System.out.println("The user guide is not available."
//    			    + "\nPlease visit the wiki at:\n"
//    			    +"http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin\n"
//    			    +"If you're online, I'll take the liberty of opening it for you.");
//    		}
//            mainPane.setSelectedComponent(scroll));
        }
        if (context.equals("MAA")){
            JPanel maaPanel = new JPanel();
            mainPane.addTab("Microarray Analysis", maaPanel);
            mainPane.setSelectedComponent(maaPanel);
        }
	getContentPane().add(mainPane, BorderLayout.CENTER);

    }
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    public int getSampleCutoffValue() {
        String text = sampleCutoffField.getText().trim();
        if (text.length() == 0)
            return 0;
        return Integer.parseInt(text);
    }
    
    public boolean shouldLinkerGenesUsed() {
        return this.useLinkerBox.isSelected();
    }
    
    public JCheckBox getUnlinkedGeneBox() {
        return this.showUnlinkedBox;
    }
    
    public boolean chooseHomoGenes() {
        return this.chooseHomoBox.isSelected();
    }
    
    public String getFileFormat() {
        if (mafBtn.isSelected())
            return "MAF";
        if (geneSampleBtn.isSelected())
            return "GeneSample";
        return "GeneSet";
    }
    
    public boolean showFIAnnotationsBeFetched() {
        return fetchFIAnnotations.isSelected();
    }
}
