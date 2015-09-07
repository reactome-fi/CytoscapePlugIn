package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.gk.util.DialogControlPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;


/**
 * This class sets up the GUIs for the various actions. Since all GUI creation
 * is handled through this class, a "context" parameter is used to determine
 * which GUI is to be implemented. Think of this as a lame GUI factory.
 * @author Eric T. Dawson
 * 
 */
@SuppressWarnings("serial")
public abstract class FIActionDialog extends JDialog {
    // Common GUI controls
    protected JTextField fileTF;
    // There should be a OK button always
    protected JButton okBtn;
    //Universal parameters
    protected boolean isOkClicked;
    
    protected FIActionDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    /**
     * Creates the graphical user interfaces for each of the Reactome FI analyses.
     * @param actionType A string indicating the type of action to be performed (GeneSetMutationAnalysis, UserGuide, Microarray, Hotnet).
     */
    private void init() {
        // Main dialog pane. A tabbed pane is used
        // to mimic the Cytoscape GUI and provide room
        // for future tabbed user interfaces.
        // The mouse event has been overridden to avoid the mouse action.
        setTitle("Reactome FI PlugIn");
        JTabbedPane mainPane = new JTabbedPane() {

            @Override
            protected void processMouseEvent(MouseEvent e) {
                // Do nothing to disable mouse clicking effect.
            }
            
        };
        setSize(500, 535);
        Font font = new Font("Verdana", Font.BOLD, 12);
        
        // Pane for FI Network version selection.
        FIVersionSelectionPanel versionPane = new FIVersionSelectionPanel();
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border versionBorder = BorderFactory.createTitledBorder(etchedBorder,
                                                                versionPane.getTitle(), TitledBorder.LEFT, TitledBorder.CENTER,
                                                                font);
        versionPane.setBorder(versionBorder);
        
        // Create a control pane first so that the OK button can be used
        // during synchronization
        DialogControlPane controlPane = initControlPane();
        
        JPanel innerPanel = createInnerPanel(versionPane,
                                             font);
        String tabTitle = getTabTitle();
        
        mainPane.addTab(tabTitle, innerPanel);
        mainPane.setSelectedComponent(innerPanel);
        
        getContentPane().add(mainPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    private DialogControlPane initControlPane() {
        DialogControlPane controlPane = new DialogControlPane();
        this.okBtn = controlPane.getOKBtn();
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOKAction();
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
        return controlPane;
    }

    /**
     * Retrieves the file given from the path in the textbox.
     * @return The file at the specified location
     */
    public File getSelectedFile() {
        String text = fileTF.getText().trim();
        if (text.length() == 0)
            return null;
        return new File(text);
    }
    
    protected Collection<FileChooserFilter> createFileFilters() {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
        // The following code to choose two file formats is not reliable.
        // The user may choose a customized file (e.g. tab delimited).
        String[] mafExts = new String[2];
        mafExts[0] = "txt";
        mafExts[1] = "maf";
        FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files",
                                                            mafExts);
        filters.add(mafFilter);
        return filters;
    }

    /**
     * Allows a user to browse for a file using Cytoscape's built-in file utility.
     * @param tf
     */
    protected void browseFile(JTextField tf) {
        Collection<FileChooserFilter> filters = createFileFilters();
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
        if (fileUtilRef == null) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot find file loading service registered in Cytoscape!",
                                          "No File Service", 
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);
        File dataFile = fileUtil.getFile(this,
                                         "Select File for Analysis", 
                                         FileUtil.LOAD, 
                                         filters);
        if (dataFile == null) 
            return;
        tf.setText(dataFile.getAbsolutePath());
        fileUtil = null;
        context.ungetService(fileUtilRef);
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
    protected void createFileChooserGui(final JTextField fileTF,
                                        final JLabel fileChooseLabel,
                                        final JButton browseButton, 
                                        JPanel loadPanel,
                                        GridBagConstraints constraints) {
        createFileChooserGui(fileTF, fileChooseLabel, browseButton, loadPanel, constraints, true);
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
    protected void createFileChooserGui(final JTextField fileTF,
                                        final JLabel fileChooseLabel,
                                        final JButton browseButton, 
                                        JPanel loadPanel,
                                        GridBagConstraints constraints,
                                        boolean needControlOkBtn) {
        double oldWeightx = constraints.weightx;
        constraints.weightx = 0.1d;
        loadPanel.add(fileChooseLabel, constraints);
        if (needControlOkBtn) {
            fileTF.getDocument().addDocumentListener(new DocumentListener(){
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    okBtn.setEnabled(fileTF.getText().trim().length() > 0);
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    okBtn.setEnabled(fileTF.getText().trim().length() > 0);
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
            // Disable okBtn as default
            okBtn.setEnabled(false);
        }
        fileTF.setColumns(20);
        constraints.gridx = 1;
        constraints.weightx = 0.8d;
        loadPanel.add(fileTF, constraints);
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                browseFile(fileTF);
            }
        });

        constraints.gridx = 2;
        constraints.weightx = 0.1d;
        loadPanel.add(browseButton, constraints);
        constraints.weightx = oldWeightx;
    }

    protected abstract JPanel createInnerPanel(FIVersionSelectionPanel versionPanel,
                                               Font font);
    
    protected abstract String getTabTitle(); 
    
    protected void doOKAction() {
        isOkClicked = true;
        dispose();
    }
    
    /**
     * 
     * @return whether the ok button has been clicked or not.
     */
    public boolean isOkClicked()
    {
        return this.isOkClicked;
    }

    
}
