package org.reactome.cytoscape.sc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.application.swing.CytoPanelName;
import org.gk.util.DialogControlPane;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.PathwayHierarchyLoadTask;
import org.reactome.cytoscape.sc.server.JSONServerCaller;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.sc.utils.Scpy4ReactomeDownloader;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to process single cell pathway activity analysis. This is a singleton to keep some pathway related
 * information.
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class PathwayActivityAnalyzer {
    private final static Logger logger = LoggerFactory.getLogger(PathwayActivityAnalyzer.class);
    private static PathwayActivityAnalyzer analyzer;
    protected Map<ScPathwayMethod, String> method2key;
    
    protected PathwayActivityAnalyzer() {
        method2key = new HashMap<>();
    }
    
    public static final PathwayActivityAnalyzer getAnalyzer() {
        if (analyzer == null)
            analyzer = new PathwayActivityAnalyzer();
        return analyzer;
    }
    
    /**
     * View pathway activities.
     */
    public PathwayActivities viewPathwayActivities(JSONServerCaller caller) throws Exception {
        if(!ensureAnalysis()) 
            return null; // Analysis has not done yet
        PathwayNameDialog dialog = createNameDialog();
        if (!dialog.isOKClicked)
            return null;
        String pathway = dialog.pathwayTF.getText().trim();
        ScPathwayMethod method = (ScPathwayMethod) dialog.methodBox.getSelectedItem();
        return viewPathwayActivities(pathway, method, caller);
    }
    
    protected PathwayNameDialog createNameDialog() {
        return new PathwayNameDialog();
    }
    
    /**
     * View pathway activities.
     */
    public PathwayActivities viewPathwayActivities(String pathway,
                                                   ScPathwayMethod method,
                                                   JSONServerCaller caller) throws Exception {
        String dataKey = method2key.get(method);
        Map<String, Double> cell2value = caller.getPathwayActivities(pathway,
                                                                     dataKey);
        PathwayActivities rtn = new PathwayActivities();
        rtn.id2value = cell2value;
        rtn.method = method;
        rtn.pathwayName = pathway;
        return rtn;
    }

    protected boolean ensureAnalysis() {
        if (method2key.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Please perform a pathway analysis first before viewing.",
                                          "No Pathway Data",
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }
    
    /**
     * Conduct pathway anova analysis
     */
    public void performANOVA(JSONServerCaller caller,
                             PathwaySpecies species) {
        if (!ensureAnalysis())
            return ; // Nothing is done yet
        // If there is only one pathway analysis, there is no need to popup a dialog
        ScPathwayMethod method = null;
        if (method2key.size() == 1)
            method = method2key.keySet().stream().findAny().get();
        else {
            AnovaDialog dialog = new AnovaDialog();
            if (dialog.isOKClicked)
                method = (ScPathwayMethod) dialog.methodBox.getSelectedItem();
        }
        if (method == null)
            return ; // Nothing to do
        ScPathwayMethod method1 = method;
        String dataKey = method2key.get(method);
        // Need to wrap into a thread since this is a very slow process
        Thread t = new Thread() {
            public void run() {
                JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                try {
                    ProgressPane progressPane = new ProgressPane();
                    progressPane.setIndeterminate(true);
                    parentFrame.setGlassPane(progressPane);
                    progressPane.setTitle("ANOVA");
                    progressPane.setVisible(true);
                    progressPane.setText("Performing anova...");
                    Map<String, Map<String, Double>> anovaResults = caller.doPathwayAnova(dataKey);
                    displayANOVA(anovaResults, method1, species, progressPane);
                    progressPane.setText("Done");
                }
                catch(Exception e) {
                    JOptionPane.showMessageDialog(parentFrame,
                                                  e.getMessage(),
                                                  "Error in ANOVA",
                                                  JOptionPane.ERROR_MESSAGE);
                    logger.error(e.getMessage(), e);
                }
                parentFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    protected void displayANOVA(Map<String, Map<String, Double>> anovaResults,
                                ScPathwayMethod method,
                                PathwaySpecies species,
                                ProgressPane progressPane) throws Exception {
        progressPane.setText("Load pathways...");
        PathwayHierarchyLoadTask pathwayLoader = new PathwayHierarchyLoadTask();
        pathwayLoader.setSpecies(species);
        pathwayLoader.displayReactomePathways(null);
        progressPane.setText("Display ANOVA results...");
        PathwayANOVAResultPane resultPane = new PathwayANOVAResultPane(PathwayControlPanel.getInstance().getEventTreePane(), 
                                                                       "Pathway ANOVA: " + method);
        resultPane.setResults(method, anovaResults);
        // Need to select it
        PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);
    }
    
    
    /**
     * Conduct pathway analysis.
     * @param species
     * @param caller
     */
    public void performAnalysis(PathwaySpecies species,
                                       JSONServerCaller caller) {
        ScPathwayMethod method = chooseMethod();
        if (method == null)
            return; // Cancelled by the user
        Thread t = new Thread() {
            public void run() {
                JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                try {
                    ProgressPane progressPane = new ProgressPane();
                    progressPane.setIndeterminate(true);
                    parentFrame.setGlassPane(progressPane);
                    progressPane.setTitle("Pathway Analysis");
                    progressPane.setVisible(true);
                    progressPane.setText("Download the GMT file...");
                    Scpy4ReactomeDownloader downloader = new Scpy4ReactomeDownloader();
                    String gmtFileName = downloader.downloadGMTFile(species);
                    progressPane.setText("Perform analysis...");
                    String dataKey = caller.doPathwayAnalysis(gmtFileName, method);
                    if (dataKey.toLowerCase().startsWith("error"))
                        throw new IllegalStateException(dataKey); // Something not right
                    progressPane.setText("Done");
                    method2key.put(method, dataKey);
                    JOptionPane.showMessageDialog(parentFrame,
                                                  "The analysis is done. Use \"Perform ANOVA\" or \"View Pathway Activities\"\n" + 
                                                  "for further visualization or analysis.",
                                                  "Pathway Analysis",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
                catch(Exception e) {
                    JOptionPane.showMessageDialog(parentFrame,
                                                  e.getMessage(),
                                                  "Error in Pathway Analysis",
                                                  JOptionPane.ERROR_MESSAGE);
                    logger.error(e.getMessage(), e);
                }
                parentFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    /**
     * Choose a pathway analysis method.
     * @return
     */
    private ScPathwayMethod chooseMethod() {
        MethodChooseDialog dialog = new MethodChooseDialog();
        if (dialog.isOKClicked)
            return (ScPathwayMethod) dialog.methodBox.getSelectedItem();
        return null;
    }
    
    private class AnovaDialog extends PathwayMethodDialog {
        
        public AnovaDialog() {
            super();
        }

        @Override
        protected String getDialogTitle() {
            return "Method for ANOVA";
        }

        @Override
        protected void customizeContentPane(JPanel contentPane, GridBagConstraints constraints) {
        }

        @Override
        protected void addMethods(JComboBox<ScPathwayMethod> methodBox) {
            for (ScPathwayMethod method : method2key.keySet()) {
                methodBox.addItem(method);
            }
        }

        @Override
        protected void setDialogSize() {
            setSize(490, 210);
        }
        
    }
    
    protected class PathwayNameDialog extends PathwayMethodDialog {
        private JTextField pathwayTF;
        
        public PathwayNameDialog() {
            super();
        }
        
        @Override
        protected String getDialogTitle() {
            return "Choose a Pathway";
        }
        
        protected String getLabelText() {
            return "Enter a pathway:";
        }

        @Override
        protected void customizeContentPane(JPanel contentPane, GridBagConstraints constraints) {
            JLabel label = new JLabel(getLabelText());
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            pathwayTF = new JTextField();
            pathwayTF.setColumns(16);
            constraints.gridy ++;
            contentPane.add(pathwayTF, constraints);
        }

        @Override
        protected void addMethods(JComboBox<ScPathwayMethod> methodBox) {
            for (ScPathwayMethod method : method2key.keySet()) {
                methodBox.addItem(method);
            }
        }

        @Override
        protected void setDialogSize() {
            setSize(490, 250);
        }
    }
    
    protected class MethodChooseDialog extends PathwayMethodDialog {
        
        public MethodChooseDialog() {
            super();
        }
        
        @Override
        protected String getDialogTitle() {
            return "Choose Analysis Method";
        }

        @Override
        protected void customizeContentPane(JPanel contentPane, GridBagConstraints constraints) {
            String note = "<html>*: For information about each method, double click <a href=\"https://www.nature.com/articles/nature08460\">ssGSEA</a> or"
                    + " <a href=\"https://www.nature.com/articles/nmeth.4463\">AUCell</a></html>";
            addNotePane(note, contentPane, constraints);
        }

        @Override
        protected void addMethods(JComboBox<ScPathwayMethod> methodBox) {
            for (ScPathwayMethod method : ScPathwayMethod.values())
                methodBox.addItem(method);
        }

        @Override
        protected void setDialogSize() {
            setSize(490, 235);
        }
        
        protected void addNotePane(String note,
                                   JPanel contentPane, 
                                   GridBagConstraints constraints) {
            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setContentType("text/html");
            editorPane.setBackground(getBackground());
            editorPane.setText(note);
            editorPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String desc = e.getDescription();
                        PlugInUtilities.openURL(desc);
                    }
                }
            });
            
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 2;
            constraints.gridheight = 1;
            contentPane.add(editorPane, constraints);
            getContentPane().add(contentPane, BorderLayout.CENTER);
        }
    }
    
    private abstract class PathwayMethodDialog extends JDialog {
        protected boolean isOKClicked;
        protected JComboBox<ScPathwayMethod> methodBox;
        
        public PathwayMethodDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        protected abstract String getDialogTitle();
        
        protected abstract void customizeContentPane(JPanel contentPane, GridBagConstraints constraints);
        
        protected abstract void addMethods(JComboBox<ScPathwayMethod> methodBox);
        
        protected abstract void setDialogSize();
        
        private void init() {
            setTitle(getDialogTitle());
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel("Choose an analysis method: ");
            constraints.gridx = 0;
            constraints.gridy = 0;
            contentPane.add(label, constraints);
            methodBox = new JComboBox<ScPathwayMethod>();
            addMethods(methodBox);
            constraints.gridx = 1;
            contentPane.add(methodBox, constraints);
            customizeContentPane(contentPane, constraints);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(e -> {
                isOKClicked = true;
                dispose();
            });
            controlPane.getCancelBtn().addActionListener(e -> dispose());
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setDialogSize();
            setModal(true);
            setLocationRelativeTo(getOwner());
            setVisible(true);
        }
    }
    
    class PathwayActivities {
        ScPathwayMethod method;
        String pathwayName;
        Map<String, Double> id2value;
    }

}
