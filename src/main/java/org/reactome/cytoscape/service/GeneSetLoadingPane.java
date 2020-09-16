package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.FileUtility;

public class GeneSetLoadingPane extends JPanel {

    final String ENTER_GENE_BUTTON_TEXT = "Click to Enter";
    boolean isOkClicked;
    protected JTextField fileNameTF;
    JRadioButton commaDelimitedBtn;
    JRadioButton tabDelimitedBtn;
    JRadioButton lineDelimitedBtn;
    protected DialogControlPane controlPane;
    protected JDialog parentDialog;
    String enteredGenes;
    JButton enterButton;
    
    /**
     * This is used only for subclassing.
     */
    protected GeneSetLoadingPane() {
        
    }
    
    public GeneSetLoadingPane(Component parent) {
        init(parent);
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    protected String getTitle() {
        return "Reactome Pathway Enrichment Analysis";
    }
    
    public Set<String> getGeneSet() throws IOException {
        String genes = getGenes();
        if (genes == null)
            return new HashSet<>();
        Set<String> geneSet = Stream.of(genes.split("\n")).collect(Collectors.toSet());
        return geneSet;
    }
    
    public String getGenes() throws IOException {
        String fileName = getSelectedFile();
        String format = getFileFormat();
        String rtn = null;
        if (fileName != null) {
            // Need to parse the genes to create a list of genes in a line delimited format
            FileUtility fu = new FileUtility();
            fu.setInput(fileName);
            StringBuilder builder = new StringBuilder();
            String line = null;
            if (format.equals("line")) {
                while ((line = fu.readLine()) != null) {
                    builder.append(line.trim()).append("\n");
                }
            }
            else {
                line = fu.readLine();
                String[] tokens = null;
                if (format.equals("comma"))
                    tokens = line.split(",( )?");
                else
                    tokens = line.split("\t");
                for (String token : tokens)
                    builder.append(token).append("\n");
            }
            fu.close();
            rtn = builder.toString();
        }
        else if (enteredGenes != null) {
            if (format.equals("line"))
                rtn = enteredGenes;
            else if (format.equals("comma")) 
                rtn = String.join("\n", enteredGenes.split(","));
            else if (format.equals("tab"))
                rtn = String.join("\n", enteredGenes.split("\t"));
        }
        return rtn;
    }
    
    public String getSelectedFile() {
        String file = this.fileNameTF.getText().trim();
        if (file.length() == 0)
            return null;
        return file;
    }
    
    private String getFileFormat() {
        if (commaDelimitedBtn.isSelected())
            return "comma";
        if (tabDelimitedBtn.isSelected())
            return "tab";
        return "line";
    }
    
    protected void validateOkButton() {
        if (getSelectedFile() != null || enteredGenes != null)
            controlPane.getOKBtn().setEnabled(true);
        else
            controlPane.getOKBtn().setEnabled(false);
    }
    
    protected void browseFile() {
        PlugInUtilities.browseFileForLoad(fileNameTF, "Gene Set File", new String[] {"txt"});
    }
    
    protected void init(Component parent) {
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setLayout(new GridBagLayout());;
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 8, 4);
        JLabel label = createTitleLabel();
        this.add(label, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        JPanel filePanel = createFilePane();
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(filePanel, constraints);
        
        JPanel enterPane = createDirectEnterPane();
        constraints.gridy ++;
        this.add(enterPane, constraints);
        
        JPanel formatPane = createFormatPane();
        constraints.gridy ++;
        this.add(formatPane, constraints);
        
        showInDialog(parent);
    }

    protected void showInDialog(Component parent) {
        parentDialog = GKApplicationUtilities.createDialog(parent, getTitle());
        parentDialog.getContentPane().add(this, BorderLayout.CENTER);
        controlPane = new DialogControlPane();
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                isOkClicked = true;
                parentDialog.dispose();
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                parentDialog.dispose();
            }
        });
        validateOkButton();
        parentDialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
        parentDialog.setLocationRelativeTo(parent);
        setDialogSize();
        parentDialog.setModal(true);
        parentDialog.setVisible(true);
    }
    
    protected void setDialogSize() {
        parentDialog.setSize(480, 280);
    }
    
    private JPanel createDirectEnterPane() {
        JPanel enterPane = new JPanel();
        enterPane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        enterPane.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel label = new JLabel("Or enter gene set: ");
        enterPane.add(label);
        enterButton = new JButton(ENTER_GENE_BUTTON_TEXT);
        // Want to extend the width of this button
        enterButton.addActionListener(e -> enterGenes());
        enterPane.add(enterButton);
        return enterPane;
    }
    
    private void enterGenes() {
        JDialog owner = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, this);
        GeneSetEnterDialog dialog = new GeneSetEnterDialog(owner, fileNameTF) {
            
            @Override
            protected void resetEnterGeneGUIs() {
                enterButton.setText(ENTER_GENE_BUTTON_TEXT);
                enteredGenes = null;
            }
        };
        if (enteredGenes != null) 
            dialog.getGeneTA().setText(enteredGenes);
        dialog.getControlPane().getOKBtn().setEnabled(false); // Have to reset it as disabled in case the original text is placed.
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked())
            return;
        String text = dialog.getGeneTA().getText().trim();
        if (text.length() == 0) {
            enterButton.setText(ENTER_GENE_BUTTON_TEXT);
            enteredGenes = null;
        }
        else {
            // Display how many genes entered
            String delim = null;
            String format = getFileFormat();
            if (format.equals("comma"))
                delim = ",";
            else if (format.equals("tab"))
                delim = "\t";
            else
                delim = "\n";
            String[] tokens = text.split(delim);
            enterButton.setText(tokens.length + " Genes Entered");
            enteredGenes = text;
            fileNameTF.setText(null); // Remove all text
            SwingUtilities.invokeLater(new Runnable() {
                
                @Override
                public void run() {
                    validateOkButton();; // The actual OK button of the whole dialog
                }
            });
        }
    }

    protected JPanel createFilePane() {
        JPanel filePanel = new JPanel();
        filePanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JLabel label = createFileLabel();
        filePanel.add(label);
        fileNameTF = new JTextField();
        fileNameTF.setEnabled(false);
        fileNameTF.setColumns(10);
        fileNameTF.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                validateOkButton();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateOkButton();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                validateOkButton();
            }
        });
        filePanel.add(fileNameTF);
        JButton browseFileBtn = new JButton("Browse");
        browseFileBtn.addActionListener(e -> browseFile());
        filePanel.add(browseFileBtn);
        return filePanel;
    }

    protected JPanel createFormatPane() {
        JLabel label;
        JPanel formatPane = new JPanel();
        formatPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        label = new JLabel("Specify file format:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        formatPane.add(label, constraints);
        commaDelimitedBtn = new JRadioButton("Comma delimited (e.g. TP53, EGFR)");
        tabDelimitedBtn = new JRadioButton("Tab delimited (e.g. TP53   EGFR)");
        lineDelimitedBtn = new JRadioButton("One gene per line");
        lineDelimitedBtn.setSelected(true); // The default file format
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(commaDelimitedBtn);
        buttonGroup.add(tabDelimitedBtn);
        buttonGroup.add(lineDelimitedBtn);
        constraints.gridx = 1;
        formatPane.add(lineDelimitedBtn, constraints);
        constraints.gridy = 1;
        formatPane.add(commaDelimitedBtn, constraints);
        constraints.gridy = 2;
        formatPane.add(tabDelimitedBtn, constraints);
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        return formatPane;
    }

    protected JLabel createFileLabel() {
        return new JLabel("Choose a gene set file:");
    }

    protected JLabel createTitleLabel() {
        return new JLabel("<html><b><u>Gene Set Loading</u></b></html>");
    }
    
}
