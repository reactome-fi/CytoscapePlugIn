/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.reactome.factorgraph.common.DataType;
import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.InteractionUtilities;

/**
 * A customized JPanel used for entering data files.
 * @author gwu
 *
 */
public abstract class ObservationDataLoadPanel extends JPanel {
    private List<JTextField> dnaTFs;
    private List<JTextField> geneExpTFs;
    private JCheckBox useTwoCasesBox;
    private JTextField twoCaseFileTF;
    
    public ObservationDataLoadPanel() {
        init(getFont());
    }
    
    public ObservationDataLoadPanel(Font font) {
        init(font);
    }
    
    private void init(Font font) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
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
        add(dnaPane);
        
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
        add(geneExpressionPane);
        
        JPanel twoCasesPane = createTwoCasesPane();
        twoCasesPane.setBorder(BorderFactory.createEtchedBorder());
        add(twoCasesPane);
        
        setValues();
    }
    
    private void setValues() {
        if (!FactorGraphRegistry.getRegistry().isDataLoaded())
            return; 
        setValues(DataType.CNV, dnaTFs);
        setValues(DataType.mRNA_EXP, geneExpTFs);
        if (FactorGraphRegistry.getRegistry().getSampleInfoFile() != null) {
            useTwoCasesBox.setSelected(true);
            twoCaseFileTF.setText(FactorGraphRegistry.getRegistry().getSampleInfoFile().getAbsolutePath());
        }
    }

    private void setValues(DataType dataType,
                           List<JTextField> tfs) {
        FactorGraphRegistry registry = FactorGraphRegistry.getRegistry();
        String fileName = registry.getLoadedDataFileName(dataType);
        double[] thresholds = registry.getLoadedThresholds(dataType);
        // The last TF is not editable. There is only two values provided in the threshold array
        for (int i = 0; i < tfs.size() - 1; i++) {
            JTextField tf = tfs.get(i);
            if (i == 0)
                tf.setText(fileName);
            else
                tf.setText(thresholds[i - 1] + "");
        }
    }
    
    private JPanel createTwoCasesPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5d;
        
        useTwoCasesBox = new JCheckBox("Used for pathway analysis for samples with two cases.");
        constraints.gridwidth = 3;
        pane.add(useTwoCasesBox, constraints);
        
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        final JLabel label = new JLabel("Choose a sample information file:");
        twoCaseFileTF = new JTextField();
        final JButton browseBtn = new JButton("Browse");
        createFileChooserGui(twoCaseFileTF, label, browseBtn, pane, constraints);
        // Add a note
        final JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBackground(getBackground());
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        Font font = ta.getFont();
        font = font.deriveFont(Font.ITALIC, font.getSize() - 1);
        ta.setFont(font);
        ta.setText("Note: A sample information file should be a text file: one line for one sample containing "
                 + "sample name and type separated by a tab, two types only, and no title line. Your analysis "
                 + "will be performed against samples in this file.");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        pane.add(ta, constraints);
        
        useTwoCasesBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean enabled = useTwoCasesBox.isSelected();
                label.setEnabled(enabled);
                twoCaseFileTF.setEnabled(enabled);
                browseBtn.setEnabled(enabled);
                ta.setEnabled(enabled);
            }
        });
        // The following should be disabled first
        label.setEnabled(false);
        twoCaseFileTF.setEnabled(false);
        browseBtn.setEnabled(false);
        ta.setEnabled(false);
        
        return pane;
    }
    
    protected abstract void createFileChooserGui(JTextField fileTF,
                                                 JLabel fileChooseLabel,
                                                 JButton browseButton, 
                                                 JPanel loadPanel,
                                                 GridBagConstraints constraints);
    
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
    
    public boolean isTwoCasesAnalysisSelected() {
        return useTwoCasesBox.isSelected();
    }
    
    public File getTwoCasesSampleInfoFile() {
        String text = twoCaseFileTF.getText().trim();
        if (text.length() == 0)
            return null;
        File file = new File(text);
        return file;
    }
    
    public File getDNAFile() {
        return getFile(dnaTFs);
    }
    
    public double[] getDNAThresholdValues() {
        String value1 = dnaTFs.get(1).getText().trim();
        String value2 = dnaTFs.get(2).getText().trim();
        return new double[]{new Double(value1), new Double(value2)};
    }
    
    public double[] getGeneExpThresholdValues() {
        String value1 = geneExpTFs.get(1).getText().trim();
        String value2 = geneExpTFs.get(2).getText().trim();
        return new double[]{new Double(value1), new Double(value2)};
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
    
    public boolean validateValues() {
        return validateDNAParameters() & validateGeneExpParameters() & validateTwoCasesParameters();
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
    
    private boolean validateTwoCasesParameters() {
        if (!isTwoCasesAnalysisSelected())
            return true;
        // Check if a file has been selected
        File file = getTwoCasesSampleInfoFile();
        if (file == null) {
            JOptionPane.showMessageDialog(this,
                                          "Please choose a sample information file for two cases analysis.",
                                          "Empty File Name", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                                          "The entered file doesn't exist: " + file.getAbsolutePath(),
                                          "Empty File", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!validateTwoCaseFile(file))
            return false;
        return true;
    }
    
    private boolean validateTwoCaseFile(File file) {
        try {
            FileUtility fu = new FileUtility();
            fu.setInput(file.getAbsolutePath());
            String line = null;
            Set<String> types = new HashSet<String>();
            while ((line = fu.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 2) {
                    JOptionPane.showMessageDialog(this,
                                                  "The number of fields in a line is not two in the sample information file: \n" + 
                                                   line,
                                                  "File Error", 
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                types.add(tokens[1]);
            }
            fu.close();
            if (types.size() > 2) {
                JOptionPane.showMessageDialog(this,
                                              "More than two types exist in the sample information file:\n" + 
                                              InteractionUtilities.joinStringElements(", ", types),
                                              "Empty File", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in reading file: " + file.getAbsolutePath(),
                                          "File Error", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
    
}
