package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.DialogControlPane;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cancer.MATFileLoader;
import org.reactome.cytoscape3.NetworkActionCollection.ClusterFINetworkTaskFactory;



import org.reactome.r3.util.InteractionUtilities;



public class HotNetAnalysisTask implements Runnable //extends AbstractTask
{

    private ActionDialogs gui;
    private File file;
    private CySwingApplication desktopApp;
    private ServiceReference networkManagerRef;
    private CyNetworkManager networkManager;
    private CyNetworkViewManager viewManager;
    private ServiceReference viewManagerRef;
    private CyNetworkViewFactory viewFactory;
    private ServiceReference viewFactoryRef;
    public HotNetAnalysisTask(ActionDialogs gui)
    {
        this.gui = gui;
        this.file = gui.getSelectedFile();
    }
   
    public void run()
    {
        getCyServices();
        FIProgressBar progBar = new FIProgressBar("HotNet Analysis");
        this.desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
//        tm.setTitle("HotNet Analysis");
//        tm.setProgress(.15);
//        tm.setStatusMessage("Loading mutation file...");
        try
        {
            progBar.setWaitCursor();
            progBar.setProgress(25);
            Map<String, Set<String>> sampleToGenes = new MATFileLoader().loadSampleToGenes(file.getAbsolutePath(), 
                    false);
            Map<String, Double> geneToScore = generateGeneScoreFromSamples(sampleToGenes);
//            tm.setStatusMessage("Doing HotNet analysis...");
//            tm.setProgress(-1);
            Double delta = null;
            Double fdrCutoff = null;
            if (gui.isAutoDeltaSelected())
                fdrCutoff = gui.getFDRCutoff();
            else
                delta = gui.getDelta();
            Integer permutationNumber = gui.getPermutationNumber();
            RESTFulFIService fiService = new RESTFulFIService();
            progBar.setProgress(50);
            Element resultElm = fiService.doHotNetAnalysis(geneToScore, 
                    delta, 
                    fdrCutoff, 
                    permutationNumber);
            HotNetResult hotNetResult = parseResults(resultElm);
//            tm.setProgress(.5);
//            tm.setStatusMessage("Choosing HotNet modules...");
            progBar.unsetWaitCursor();
            List<HotNetModule> selectedModules = displayModules(hotNetResult);
            if (selectedModules != null && !selectedModules.isEmpty())
            {
//                tm.setProgress(.75);
//                tm.setStatusMessage("Building FI Network...");
                buildFINetwork(selectedModules,
                        sampleToGenes);
            }
            
        }
        
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null,
                    "Error in HotNet Mutation Analysis: " + e.getMessage(),
                    "Error in HotNet",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        progBar.setVisible(false);
        progBar.dispose();
        releaseCyServices();

    }

    private void buildFINetwork(List<HotNetModule> modules, Map<String, Set<String>> sampleToGenes) throws Exception
    {
        Set<String> allGenes = new HashSet<String>();
        for (HotNetModule module : modules)
            allGenes.addAll(module.genes); 
        FINetworkService fiService = PlugInScopeObjectManager.getManager().getNetworkService();
        Set<String> fis = fiService.buildFINetwork(allGenes, false);
        FINetworkGenerator generator = new FINetworkGenerator();
        CyNetwork network = generator.constructFINetwork(fis);
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "FI Network for HotNet Analysis");
        if (network == null || network.getNodeCount() <= 0)
        {
            JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                    "Cannot find any functional interaction among provided genes.\n"
                            + "No network can be constructed.\n"
                            + "Note: only human gene names are supported.",
                    "Empty Network", JOptionPane.INFORMATION_MESSAGE);
            desktopApp.getJFrame().getGlassPane().setVisible(false);
            return;
        }
        networkManager.addNetwork(network);
        CyNetworkView view = viewFactory.createNetworkView(network);
        viewManager.addNetworkView(view);
        
        try
        {
//            NetworkActionCollection nac = new NetworkActionCollection();
//            ClusterFINetworkTaskFactory clusterFactory = nac.new ClusterFINetworkTaskFactory(
//                    view);
            BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
//            ServiceReference taskMgrRef = context
//                    .getServiceReference(TaskManager.class.getName());
//            TaskManager taskMgr = (TaskManager) context
//                    .getService(taskMgrRef);
//            taskMgr.execute(clusterFactory.createTaskIterator());
//            context.ungetService(taskMgrRef);
            ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
            FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context.getService(servRef);
            visStyler.setVisualStyle(view);
            visStyler.setLayout();
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null, "The visual style could not be applied.", "Visual Style Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
    }
    private Map<String, Double> generateGeneScoreFromSamples(Map<String, Set<String>> sampleToGenes)
    {
        int totalSamles = sampleToGenes.size();
        Map<String, Set<String>> geneToSamples = InteractionUtilities.switchKeyValues(sampleToGenes);
        Map<String, Double> geneToScore = new HashMap<String, Double>();
        for (String gene : geneToSamples.keySet()) {
            Set<String> samples = geneToSamples.get(gene);
            geneToScore.put(gene, (double) samples.size() / totalSamles);
        }
        return geneToScore;
    }
    @SuppressWarnings("unchecked")
    private HotNetResult parseResults(Element resultElm) {
        try {
            XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
            output.output(resultElm, System.out);
        }
        catch(IOException e) {}
        List<HotNetModule> modules = new ArrayList<HotNetModule>();
        HotNetResult result = new HotNetResult();
        result.modules = modules;
        List<Element> children = resultElm.getChildren();
        for (Element child : children) {
            String elmName = child.getName();
            if (elmName.equals("modules")) {
                HotNetModule module = new HotNetModule();
                Set<String> genes = new HashSet<String>();
                List<Element> content = child.getChildren();
                for (Element c : content) {
                    String name = c.getName();
                    if (name.equals("fdr"))
                        module.fdr = new Double(c.getText());
                    else if (name.equals("genes"))
                        genes.add(c.getText());
                    else if (name.equals("pvalue"))
                        module.pvalue = new Double(c.getText());
                }
                module.genes = genes;
                modules.add(module);
            }
            else if (elmName.equals("delta"))
                result.delta = new Double(child.getTextTrim());
            else if (elmName.equals("fdrThreshold"))
                result.fdrCutoff = new Double(child.getText());
            else if (elmName.equals("permutation"))
                result.permutation = new Integer(child.getTextTrim());
            else if (elmName.equals("useAutoDelta"))
                result.useAutoDelta = new Boolean(child.getTextTrim());
        }
        return result;
    }
    private List<HotNetModule> displayModules(HotNetResult result) {
        ResultDialog resultDialog = new ResultDialog();
        resultDialog.showResults(result);
        resultDialog.setModal(true);
        resultDialog.toFront();
        resultDialog.setVisible(true);
        if (!resultDialog.isOkClicked)
            return null;
        return resultDialog.getSelectedModules();
    }
    static class HotNetModule {
        Set<String> genes;
        Double pvalue;
        Double fdr;
    }
    
    static class HotNetResult {
        List<HotNetModule> modules;
        Double fdrCutoff;
        Integer permutation;
        Double delta;
        Boolean useAutoDelta;
    }
    private class ResultDialog extends JDialog {
        private JTable resultTable;
        private boolean isOkClicked;
        
        private JLabel numberLabel;
        private JTextField sizeTF;
        private JTextField fdrTF;
        private JLabel selectedClusterLabel;
        
        public ResultDialog() {
            super(desktopApp.getJFrame());
            setTitle("HotNet Mutation Analysis Results");
            init();
        }
        
        private void init() {
            JPanel northPane = createNorthPane();
            getContentPane().add(northPane, BorderLayout.NORTH);
            
            resultTable = new JTable();
            getContentPane().add(new JScrollPane(resultTable), BorderLayout.CENTER);
            
            // Control panel
            DialogControlPane controlPane = new DialogControlPane();
            JButton okBtn = controlPane.getOKBtn();
            okBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    isOkClicked = true;
                }
            });
            // Don't enable default button. Return is used for filtering.
            //            okBtn.setDefaultCapable(true);
            //            getRootPane().setDefaultButton(okBtn);
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    isOkClicked = false;
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(715, 560);
            setLocationRelativeTo(getOwner());
        }
        
        private JPanel createNorthPane() {
            // Top panel to show filters
            JPanel northPane = new JPanel();
            northPane.setLayout(new GridBagLayout());
            Border border = BorderFactory.createEmptyBorder(4, 4, 4, 4);
            northPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), border));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridwidth = 4;
            constraints.weightx = 0.5d;
            numberLabel = new JLabel("Total modules from clustering: ");
            northPane.add(numberLabel, constraints);
            JLabel filterLabel = new JLabel("Choose filters (presss \"return\" to do filtering after entering numbers):");
            constraints.gridy = 1;
            northPane.add(filterLabel, constraints);
            JLabel sizeLabel = new JLabel("    Size (default 3): ");
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.weightx = 0.0d;
            northPane.add(sizeLabel, constraints);
            sizeTF = new JTextField("3");
            sizeTF.setColumns(6);
            constraints.gridx = 1;
            northPane.add(sizeTF, constraints);
            JLabel corrLabel = new JLabel("FDR (false directory rate, default 0.10): ");
            constraints.gridx = 2;
            northPane.add(corrLabel, constraints);
            fdrTF = new JTextField("0.10");
            fdrTF.setColumns(6);
            constraints.gridx = 3;
            northPane.add(fdrTF, constraints);
            // Used to show the total numbers of selected clusters and genes in clusters
            selectedClusterLabel = new JLabel("Total modules in table: ");
            constraints.gridwidth = 4;
            constraints.gridx = 0;
            constraints.gridy = 3;
            northPane.add(selectedClusterLabel, constraints);
            sizeTF.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    filterModules();
                }
            });
            fdrTF.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    filterModules();
                }
            });
            return northPane;
        }
        
        public void showResults(HotNetResult result) {
            HotNetTableModel model = new HotNetTableModel();
            model.setModules(result.modules);
            resultTable.setModel(model);
            String label = numberLabel.getText();
            int index = label.indexOf(":");
            StringBuilder builder = new StringBuilder();
            builder.append(result.modules.size()).append(" based on delta ");
            builder.append(result.delta);
            if (result.useAutoDelta)
                builder.append(" via auto-delta (fdr=").append(result.fdrCutoff).append(") ");
            builder.append(" with ").append(result.permutation).append(" permutations.");
            label = label.substring(0, index) + ": " + builder.toString();
            numberLabel.setText(label);
            filterModules();
        }
        
        public List<HotNetModule> getSelectedModules() {
            HotNetTableModel model = (HotNetTableModel) resultTable.getModel();
            return model.getDisplayedModules();
        }
        
        private void filterModules() {
            double fdr = new Double(fdrTF.getText().trim());
            int size = new Integer(sizeTF.getText().trim());
            HotNetTableModel model = (HotNetTableModel) resultTable.getModel();
            model.filterModules(size, fdr);
            // Show some information
            int totalModule = model.getTotalModules();
            int totalGenes = model.getTotalGenes().size();
            String label = selectedClusterLabel.getText();
            int index = label.indexOf(":");
            label = label.substring(0, index) + ": " + totalModule + " (" + totalGenes + " genes)";
            selectedClusterLabel.setText(label);
        }
        
    }
    private class HotNetTableModel extends AbstractTableModel {
        private String[] headers = new String[]{
                "Module Index",
                "Size",
                "p-value",
                "FDR",
                "Genes"
        };
        private List<HotNetModule> modules;
        private List<HotNetModule> displayedModules;
        
        public HotNetTableModel() {
            displayedModules = new ArrayList<HotNetModule>();
        }
        
        public void setModules(List<HotNetModule> modules) {
            this.modules = modules;
            displayedModules.clear();
            displayedModules.addAll(modules);
            fireTableDataChanged();
        }
        
        public void filterModules(int size,
                                  double fdr) {
            displayedModules.clear();
            for (HotNetModule module : modules) {
                if (module.genes.size() >= size && module.fdr <= fdr)
                    displayedModules.add(module);
            }
            fireTableDataChanged();
        }
        
        
        public int getTotalModules() {
            return displayedModules.size();
        }
        
        public Set<String> getTotalGenes() {
            Set<String> genes = new HashSet<String>();
            for (HotNetModule module : displayedModules)
                genes.addAll(module.genes);
            return genes;
        }
        
        public List<HotNetModule> getDisplayedModules() {
            return this.displayedModules;
        }

        @Override
        public int getRowCount() {
            return displayedModules.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HotNetModule module = displayedModules.get(rowIndex);
            switch (columnIndex) {
                case 0 :
                    return rowIndex;
                case 1 :
                    return module.genes.size();
                case 2 :
                    return module.pvalue;
                case 3 :
                    return module.fdr;
                case 4 :
                    return InteractionUtilities.joinStringElements(",", module.genes);
            }
            return null;
        }
        
    }
    private void getCyServices()
    {
        //Get CyNetworkManager
        Map<ServiceReference,  Object> netManagerRefToObj =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyNetworkManager.class.getName());
        ServiceReference servRef = (ServiceReference) netManagerRefToObj.keySet().toArray()[0];
        CyNetworkManager netManager = (CyNetworkManager) netManagerRefToObj.get(servRef);
        this.networkManager = netManager;
        this.networkManagerRef = servRef;
        
        //Get CyNetworkViewFactory
        Map<ServiceReference, Object> viewFactoryRefToObj = PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyNetworkViewFactory.class.getName());
        servRef = (ServiceReference) viewFactoryRefToObj.keySet().toArray()[0];
        CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) viewFactoryRefToObj.get(servRef);
        this.viewFactory = viewFactory;
        this.viewFactoryRef = servRef;
        //Get CyNetworkViewManager
        Map<ServiceReference,  Object> viewManagerRefToObj =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyNetworkViewManager.class.getName());
        servRef = (ServiceReference) viewManagerRefToObj.keySet().toArray()[0];
        CyNetworkViewManager viewManager = (CyNetworkViewManager) viewManagerRefToObj.get(servRef);
        this.viewManager = viewManager;
        this.viewManagerRef = servRef;
       
    }
    private void releaseCyServices()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        context.ungetService(networkManagerRef);
        context.ungetService(viewFactoryRef);
        context.ungetService(viewManagerRef);
    }
}
