package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.DialogControlPane;
import org.gk.util.ProgressPane;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.InteractionUtilities;



/**
 * This class is used to do a module-based survival analysis. The actual survival
 * analysis is done on the server-side using an R script. This class basically is a
 * front-end of that R script.
 * @author wgm ported August 2013 Eric T Dawson
 *
 */
public class ModuleBasedSurvivalAnalysisHelper
{
    private final String CLINICAL_FILE_PROP_KEY = "clinicalFile";
    // Cache this value for single module based survival analysis. This may be heavy.
    // Note: Need to make sure only one ResultSurvivalPanel object has been used by one and only
    // one network. Otherwise, the cached scoreMatric may not be used by a single module-based
    // data analysis: the result from a single module based analysis may not be the same from coxph.
    // In the current implementation, it should be safe even though multiple copies of objects
    // are used in the plug-in.
    private String scoreMatrix;
    private FileUtil fileUtil;
    public ModuleBasedSurvivalAnalysisHelper()
    {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(FileUtil.class.getName());
        this.fileUtil = (FileUtil) context.getService(servRef);
    }
    public ModuleBasedSurvivalAnalysisHelper(CyNetworkView view)
    {
        //this.view = view;
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(FileUtil.class.getName());
        this.fileUtil = (FileUtil) context.getService(servRef);
    }
    
    public void doSurvivalAnalysis(Map<String, Integer> nodeToModule,
                                   Map<String, Set<String>> nodeToSampleSet) throws IOException
                                   {
        // TODO Auto-generated method stub
        final String scoreMatrix = generateScoreMatrix(nodeToModule, nodeToSampleSet);
        doSurvivalAnalysis(nodeToModule, 
                           null,
                           scoreMatrix);
                                   }
    
    public void doSurvivalAnalysisForMCLModules(
                                                Map<String, Integer> nodeToModule,
                                                Map<Integer, Map<String, Double>> moduleToSampleToValue) throws IOException
                                                {
        // TODO Auto-generated method stub
        Set<Integer> selectedModules = new HashSet<Integer>(nodeToModule.values());
        Map<Integer, Map<String, Double>> copy = new HashMap<Integer, Map<String, Double>>(moduleToSampleToValue);
        //Filter out unselected modules.
        for (Iterator<Integer> it = copy.keySet().iterator(); it.hasNext();) 
        {
            Integer module = it.next();
            if (!(selectedModules.contains(module)))
                it.remove();
        }
        String scoreMatrix = generateScoreMatrixForMCL(moduleToSampleToValue);
        //System.out.println(nodeToModule + "\n\n\n\n");
        //System.out.println(scoreMatrix + "\n\n score matrix");
        doSurvivalAnalysis(nodeToModule, 
                           null,
                           scoreMatrix);
                                                }
    
    /**
     * This helper method is used to generate a String object containing a matrix
     * from sample to scores.
     * @param nodeToModule
     * @param nodeToSamples
     * @return
     */
    private String generateScoreMatrix(Map<String, Integer> nodeToModule,
                                       Map<String, Set<String>> nodeToSamples) {
        StringBuilder builder = new StringBuilder();
        // Title
        builder.append("Sample");
        List<Integer> moduleList = new ArrayList<Integer>(new HashSet<Integer>(nodeToModule.values()));
        Collections.sort(moduleList);
        for (Integer module : moduleList) {
            builder.append("\t").append(module);
        }
        builder.append("\n");
        Map<String, Set<String>> sampleToNodes = InteractionUtilities.switchKeyValues(nodeToSamples);
        List<String> sampleList = new ArrayList<String>(sampleToNodes.keySet());
        Set<Integer> sampleModules = new HashSet<Integer>();
        for (String sample : sampleList) {
            Set<String> nodes = sampleToNodes.get(sample);
            builder.append(sample);
            sampleModules.clear();
            for (String node : nodes) {
                Integer module = nodeToModule.get(node);
                if (module != null)
                    sampleModules.add(module);
            }
            for (Integer module : moduleList) {
                builder.append("\t");
                if (sampleModules.contains(module))
                    builder.append("1");
                else
                    builder.append("0");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
    
    private String generateScoreMatrixForMCL(Map<Integer, Map<String, Double>> moduleToSampleToValue) {
        StringBuilder builder = new StringBuilder();
        List<Integer> modules = new ArrayList<Integer>(moduleToSampleToValue.keySet());
        Collections.sort(modules);
        builder.append("Sample");
        for (Integer module : modules)
            builder.append("\t").append(module);
        builder.append("\n");
        // Get a sample list
        Map<String, Double> sampleToValue = moduleToSampleToValue.get(modules.get(0));
        List<String> sampleList = new ArrayList<String>(sampleToValue.keySet());
        Collections.sort(sampleList);
        for (String sample : sampleList) {
            builder.append(sample);
            for (Integer module : modules) {
                sampleToValue = moduleToSampleToValue.get(module);
                builder.append("\t").append(sampleToValue.get(sample));
            }
            builder.append("\n"); // One line for one sample
        }
        return builder.toString();
    }
    
    private void doSurvivalAnalysis(Map<String, Integer> nodeToModule,
                                    Integer selectedModule, 
                                    String scoreMatrixText) throws IOException {   
        this.scoreMatrix = scoreMatrixText;
        SurvivalInfoDialog dialog = new SurvivalInfoDialog();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setModal(true);
        dialog.setSize(525, 252);
        if (nodeToModule != null)
            dialog.setModules(nodeToModule);
        else if (selectedModule != null)
        {
            dialog.setModule(selectedModule);
            //Use the Kaplan-Meyer plot as the default.
            dialog.kmBtn.setSelected(true);
        }
        dialog.setVisible(true);
        if(!dialog.isOkClicked)
            return;
        //Retrieve clinical information from file.
        String clinInfoFile = dialog.clinFileTF.getText().trim();
        String model = null;
        if (dialog.kmBtn.isSelected())
            model = "kaplan-meier";
        else
            model = "coxph"; // This is used as default
        final String modelParam = model;
        String moduleName = dialog.moduleList.getSelectedItem().toString();
        Integer moduleIndex = null;
        if (moduleName.length() > 0)
            moduleIndex = new Integer(moduleName);
        final Integer moduleParam = moduleIndex;
        final String clinInfoMatrix = loadClinInfo(clinInfoFile);
        final String label = dialog.generateLabel();
        Thread t = new Thread()
        {
            public void run()
            {
                ProgressPane progressPane = new ProgressPane();
                progressPane.setIndeterminate(true);
                JFrame desktop = PlugInObjectManager.getManager().getCytoscapeDesktop();
                desktop.setGlassPane(progressPane);
                progressPane.setText("Doing survival analysis. Please wait...");
                desktop.getGlassPane().setVisible(true);
                try
                {
                    RESTFulFIService fiService = new RESTFulFIService();
                    Element result = fiService.doSurvivalAnalysis(scoreMatrix, clinInfoMatrix, modelParam, moduleParam);
                    displaySurvivalAnalysisResult(label, result);
                }
                catch (Exception e)
                {
                    PlugInUtilities.showErrorMessage("Error in Survival Analysis", "Error in analysis: " + e.getMessage());
                    e.printStackTrace();
                }
                progressPane.setIndeterminate(false);
                desktop.getGlassPane().setVisible(false);
            }
        };
        t.start();
                                    }
    
    private String loadClinInfo(String clinInfoFile) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        FileUtility fu = new FileUtility();
        fu.setInput(clinInfoFile);
        String line = null;
        while ((line = fu.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }
    
    private void displaySurvivalAnalysisResult(String label,
                                               Element result) throws IOException, BadLocationException {
        List<?> list = result.getChildren();
        String output = null;
        String error = null;
        String plotFileName = null;
        String plotResult = null;
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String name = elm.getName();
            String text = elm.getTextTrim();
            if (text != null && text.length() == 0)
                text = null; // null used as a flag
            if (name.equals("output")) {
                output = text;
            }
            else if (name.equals("error")) {
                error = text;
            }
            else if (name.equals("plotFileName")) {
                plotFileName = text;
            }
            else if (name.equals("plotResult")) {
                plotResult = text;
            }
        }
        File plotFile = null;
        if (plotFileName != null && plotResult != null)
            plotFile = savePlotResult(plotFileName, plotResult);
        displayResults(label, output, error, plotFile);
    }
    
    private File savePlotResult(String plotFileName, String plotResult) throws IOException
    {
        // TODO Auto-generated method stub
        String tDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tDir, plotFileName);
        tempFile.deleteOnExit();
        FileUtility fu = new FileUtility();
        fu.decodeInBase64(plotResult, tempFile.getAbsolutePath());
        return tempFile;
    }
    
    private void displayResults(String title,
                                String output,
                                String error,
                                File plotFile) throws IOException, BadLocationException {
        String tabTitle = SurvivalAnalysisResultPane.TITLE;
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tabbedPane = desktopApp.getCytoPanel(CytoPanelName.EAST);
        // Check if it is there
        int componentIndex = PlugInUtilities.getCytoPanelComponent(tabbedPane, tabTitle);
        // This will guaranteer to create resultPane
        SurvivalAnalysisResultPane resultPane = PlugInUtilities.getCytoPanelComponent(SurvivalAnalysisResultPane.class,
                                                                                      CytoPanelName.EAST,
                                                                                      tabTitle);
        if (componentIndex == -1) {
            // Add result
            addSingleModuleAnalysisAction(resultPane);
        }
        String[] results = new String[3];
        results[0] = output;
        results[1] = error;
        if (plotFile != null)
            results[2] = plotFile.getAbsolutePath();
        resultPane.appendResult(title, 
                                results);
    }
    
    private void addSingleModuleAnalysisAction(final SurvivalAnalysisResultPane resultPane) {
        SingleModuleSurvivalActionListener action = new SingleModuleSurvivalActionListener() {
            @Override
            public void doSingleModuleSurvivalAnalysis(SingleModuleSurvivalAnalysisActionEvent e) {
                String module = e.getModule();
                doSingleModuleAnalysis(module);
            }
        };
        resultPane.setSingleModuleSurivivalAnalysisActionListener(action);
    }
    
    private void doSingleModuleAnalysis(String module) {
        try {
            Integer moduleIndex = new Integer(module);
            doSurvivalAnalysis(null, 
                               moduleIndex,
                               scoreMatrix);
        }
        catch(NumberFormatException e) {
            PlugInUtilities.showErrorMessage(
                                             "Cannot find module index from text link.",
                    "Error in Survival Analysis");
            e.printStackTrace();
        }
        catch(IOException ioe) {
            PlugInUtilities.showErrorMessage(
                                             "Error in single module survival analysis: " + ioe,
                    "Error in Survival Analysis");
            ioe.printStackTrace();
        }
    }
    
    /**
     * A customized JDialog to gather information for survival analysis.
     */
    private class SurvivalInfoDialog extends JDialog {
        private boolean isOkClicked;
        private JComboBox moduleList;
        private JTextField clinFileTF;
        private JRadioButton coxphBtn;
        private JRadioButton kmBtn;
        
        public SurvivalInfoDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        public void setModules(Map<String, Integer> nodeToModule) {
            // Extract module information
            Set<Integer> set = new HashSet<Integer>(nodeToModule.values());
            List<Integer> list = new ArrayList<Integer>(set);
            Collections.sort(list);
            moduleList.removeAllItems();
            moduleList.addItem("");
            for (Integer module : list)
                moduleList.addItem(module);
        }
        
        public void setModule(Integer module) {
            moduleList.removeAllItems();
            moduleList.addItem(module + "");
        }
        
        private void init() {
            setTitle("Module-based Surival Analysis");
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            // The following controls are for clinical information
            JLabel clinFileLable = new JLabel("Enter survival information file: ");
            clinFileTF = new JTextField();
            clinFileTF.setText("Double click to select a file...");
            clinFileTF.setToolTipText("Double click to select a file");
            clinFileTF.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Select a file from the file system
                        //                        File[] files = FileUtil.getFiles(Cytoscape.getDesktop(),
                        //                                                         "Select Survival Information File",
                        //                                                         FileUtil.LOAD,
                        //                                                         new CyFileFilter[]{new CyFileFilter("txt")});
                        //                        if (files == null || files.length == 0)
                        //                            return;
                        //                        File file = files[0];
                        Collection<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
                        filters.add(new FileChooserFilter("Survival Information File", "txt"));
                        new FileChooserFilter("Survival Information File", "txt");
                        File file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                     "Select Survival Information File", FileUtil.LOAD, filters);
                        clinFileTF.setText(file.getAbsolutePath());
                    }
                }
            });
            clinFileTF.setColumns(50);
            // Set value
            Properties prop = PlugInObjectManager.getManager().getProperties();
            if (prop != null && prop.getProperty(CLINICAL_FILE_PROP_KEY) != null) {
                String fileName = prop.getProperty(CLINICAL_FILE_PROP_KEY);
                clinFileTF.setText(fileName);
            }
            constraints.anchor = GridBagConstraints.EAST;
            contentPane.add(clinFileLable, constraints);
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = 2;
            constraints.weightx = 0.7d;
            contentPane.add(clinFileTF, constraints);
            // Add a note about requirement of the clinical information file
            JTextArea noteTA = new JTextArea();
            Font font = contentPane.getFont();
            noteTA.setFont(font.deriveFont(font.getSize2D() - 2.0f));
            noteTA.setEditable(false);
            noteTA.setLineWrap(true);
            noteTA.setWrapStyleWord(true);
            noteTA.setBackground(contentPane.getBackground());
            noteTA.setText("Note: At least three columns should be provided in a tab-delimited " +
                    "text file containing survival information: Sample, OSEVENT, OSDURATION.");
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 3;
            constraints.gridheight = 2;
            contentPane.add(noteTA, constraints);
            
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.weightx = 0.0d;
            constraints.gridheight = 1;
            // The following controls are for selecting models
            JLabel modelLabel = new JLabel("Choose a survival analysis model: ");
            coxphBtn = new JRadioButton("coxph");
            coxphBtn.setSelected(true);
            kmBtn = new JRadioButton("Kaplan-Meier");
            ButtonGroup btnGroup = new ButtonGroup();
            btnGroup.add(coxphBtn);
            btnGroup.add(kmBtn);
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.anchor = GridBagConstraints.EAST;
            contentPane.add(modelLabel, constraints);
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            contentPane.add(coxphBtn, constraints);
            constraints.gridx = 2;
            contentPane.add(kmBtn, constraints);
            // The following controls are for select modules
            JLabel moduleLabel = new JLabel("Select a module: ");
            moduleList = new JComboBox();
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.anchor = GridBagConstraints.EAST;
            contentPane.add(moduleLabel, constraints);
            constraints.gridx = 1;
            //            constraints.gridwidth = 2;
            constraints.anchor = GridBagConstraints.CENTER;
            moduleList.addItem(""); // The first should be empty
            for (int i = 0; i < 10; i++)
                moduleList.addItem(i);
            contentPane.add(moduleList, constraints);
            // Add a control panel
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateFields()) {
                        isOkClicked = true;
                        // Need to save the clinical file
                        String fileName = clinFileTF.getText().trim();
                        Properties prop = PlugInObjectManager.getManager().getProperties();
                        if (prop != null) {
                            // Store for temp
                            prop.setProperty(CLINICAL_FILE_PROP_KEY,
                                             fileName);
                        }
                        dispose();
                    }
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            // Add these two panels
            contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                                     BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            getContentPane().add(contentPane, BorderLayout.CENTER);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
        }
        
        private boolean validateFields() {
            String text = clinFileTF.getText().trim();
            if (text.length() == 0 || text.equals("Double click to select a file...")) {
                JOptionPane.showMessageDialog(this,
                                              "You have to enter a valid clinical information file to do surival analysis.", 
                                              "No Clinical Information File Specified", 
                                              JOptionPane.ERROR_MESSAGE);
                clinFileTF.requestFocus();
                return false;
            }
            // If Kaplan-Meier model is selected, a module should be selected too
            if (kmBtn.isSelected()) {
                Object value = moduleList.getSelectedItem();
                if (value.equals("")) {
                    JOptionPane.showMessageDialog(this,
                                                  "You have to choose a module for Kaplan-Meier analysis.",
                                                  "No Module Specified",
                                                  JOptionPane.ERROR_MESSAGE);
                    moduleList.requestFocus();
                    return false;
                }
            }
            return true;
        }
        
        public String generateLabel() {
            StringBuilder builder = new StringBuilder();
            if (coxphBtn.isSelected())
                builder.append("Coxph");
            else
                builder.append("Kaplan-Meier");
            if (moduleList.getSelectedItem().equals(""))
                builder.append(" (all modules)");
            else
                builder.append(" (module ").append(moduleList.getSelectedItem()).append(")");
            return builder.toString();
        }
        
    }
}
