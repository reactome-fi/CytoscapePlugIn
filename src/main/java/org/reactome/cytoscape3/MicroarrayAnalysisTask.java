package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.gk.util.DialogControlPane;
import org.gk.util.ProgressPane;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cancer.CancerGeneExpressionCommon;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.InteractionUtilities;

public class MicroarrayAnalysisTask extends FIAnalysisTask {
    private MicroArrayAnalysisDialog gui;
    
    public MicroarrayAnalysisTask(MicroArrayAnalysisDialog gui) {
        this.gui = gui;
    }
    
    @Override
    protected void doAnalysis() {
        ProgressPane progPane = new ProgressPane();
        progPane.setTitle("Microarray Data Analysis");
        progPane.setMinimum(1);
        progPane.setMaximum(100);
        JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        parentFrame.setGlassPane(progPane);
        progPane.setVisible(true);
        progPane.setValue(25);
        progPane.setText("Loading microarray file...");
        progPane.setIndeterminate(true);
        CancerGeneExpressionCommon arrayHelper = new CancerGeneExpressionCommon();
        try {
            String fileName = gui.getSelectedFile().getPath().trim();
            Map<String, Map<String, Double>> geneToSampleToValue = arrayHelper
                    .loadGeneExp(fileName);
            FINetworkService networkService = FIPlugInHelper
                    .getHelper().getNetworkService();
            Set<String> fis = networkService.queryAllFIs();
            Set<String> fisWithCorrs = arrayHelper.calculateGeneExpCorrForFIs(geneToSampleToValue,
                                                                              fis,
                                                                              gui.shouldAbsCorUsed(),
                                                                              null);
            progPane.setIndeterminate(true);
            progPane.setText("Clustering FI network...");
            RESTFulFIService fiService = new RESTFulFIService();
            Element resultElm = fiService.doMCLClustering(fisWithCorrs,
                                                          new Double(gui.getInflation()));
            List<Set<String>> clusters = parseClusterResults(resultElm);
            // Don't want to display any small clusters
            filterSmallClusters(clusters);
            List<Double> correlations = calculateAverageCorrelations(clusters,
                                                                     fis, fisWithCorrs);
            final Map<Set<String>, Double> selectedClusterToCorr = displayClusters(
                                                                                   clusters, correlations);
            if (selectedClusterToCorr != null) {
                List<Set<String>> selectedClusters = new ArrayList<Set<String>>(
                        selectedClusterToCorr.keySet());
                Collections.sort(selectedClusters,
                                 new Comparator<Set<String>>()
                                 {
                    @Override
                    public int compare(Set<String> set1,
                                       Set<String> set2)
                    {
                        int rtn = set2.size() - set1.size();
                        if (rtn != 0) return rtn;
                        // Try to sort based on average correlation
                        Double value1 = selectedClusterToCorr.get(set1);
                        if (value1 == null)
                        {
                            value1 = 0.0d;
                        }
                        Double value2 = selectedClusterToCorr.get(set2);
                        if (value2 == null)
                        {
                            value2 = 0.0d;
                        }
                        return value2.compareTo(value1);
                    }
                                 });
                progPane.setText("Building FI network...");
                CyNetwork network = buildNetwork(selectedClusters, fis,
                                                 geneToSampleToValue);
                BundleContext context = PlugInObjectManager.getManager().getBundleContext();
                CyNetworkManager networkManager = (CyNetworkManager) context.getService(netManagerRef);
                networkManager.addNetwork(network);
                CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) context.getService(viewFactoryRef);
                CyNetworkView view = viewFactory.createNetworkView(network);
                CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(viewManagerRef);
                viewManager.addNetworkView(view);
                EdgeActionCollection.setEdgeNames(view);
                ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
                FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context.getService(servRef);
                visStyler.setVisualStyle(view);
                visStyler.setLayout();
                context.ungetService(servRef);
                ResultDisplayHelper.getHelper().showMCLModuleInTab(selectedClusters,
                                                                   selectedClusterToCorr,
                                                                   view);
            }
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(parentFrame, 
                                          "Error in MCL clustering: " + e.getMessage(), 
                                          "Error in MCL Clustering",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        parentFrame.getGlassPane().setVisible(false);
    }
    
    private CyNetwork buildNetwork(List<Set<String>> clusters,
                                   Set<String> allFIs,
                                   Map<String, Map<String, Double>> geneToSampleToValue) {
        Set<String> allGenes = new HashSet<String>();
        for (Set<String> cluster : clusters)
        {
            allGenes.addAll(cluster);
        }
        Set<String> fis = InteractionUtilities.getFIs(allGenes, allFIs);
        FINetworkGenerator generator = new FINetworkGenerator();
        CyNetwork network = generator.constructFINetwork(fis);
        Map<String, Integer> nodeToCluster = new HashMap<String, Integer>();
        for (int i = 0; i < clusters.size(); i++)
        {
            Set<String> cluster = clusters.get(i);
            for (String gene : cluster)
            {
                nodeToCluster.put(gene, i);
            }
        }
        Map<Integer, Map<String, Double>> moduleToSampleToValue = generateModuleToSampleToValue(
                                                                                                clusters, geneToSampleToValue);
        FIPlugInHelper.getHelper().storeMCLModuleToSampleToValue(moduleToSampleToValue);
        TableHelper tableHelper = new TableHelper();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "FI Network for MCL Modules");
        tableHelper.storeNodeAttributesByName(network, "module", nodeToCluster);
        tableHelper.storeClusteringType(network, 
                                        TableFormatterImpl.getMCLArrayClustering());
        tableHelper.storeFINetworkVersion(network,
                                          PlugInObjectManager.getManager().getFiNetworkVersion());
        // tableHelper.storeMCLModuleToSampleToValue(network,
        //        moduleToSampleToValue);
        tableHelper.storeDataSetType(network, TableFormatterImpl
                                     .getMCLArrayClustering());
        tableHelper.markAsFINetwork(network);
        return network;
    }
    
    private Map<Integer, Map<String, Double>> generateModuleToSampleToValue(List<Set<String>> clusters,
                                                                            Map<String, Map<String, Double>> geneToSampleToValue) {
        Map<Integer, Map<String, Double>> moduleToSampleToValue = new HashMap<Integer, Map<String, Double>>();
        List<String> samples = new ArrayList<String>();
        Map<String, Double> sampleToValue = geneToSampleToValue.values()
                .iterator().next();
        for (String sample : sampleToValue.keySet())
        {
            samples.add(sample);
        }
        double total = 0.0d;
        int count = 0;
        for (int i = 0; i < clusters.size(); i++)
        {
            Set<String> cluster = clusters.get(i);
            Map<String, Double> sampleToValue1 = new HashMap<String, Double>();
            moduleToSampleToValue.put(i, sampleToValue1);
            for (String sample : samples)
            {
                total = 0.0d;
                count = 0;
                for (String gene : cluster)
                {
                    sampleToValue = geneToSampleToValue.get(gene);
                    if (sampleToValue == null)
                    {
                        continue;
                    }
                    Double v = sampleToValue.get(sample);
                    if (v != null)
                    {
                        total += v;
                        count++;
                    }
                }
                sampleToValue1.put(sample, total / count);
            }
        }
        return moduleToSampleToValue;
                                                                            }
    
    private Map<Set<String>, Double> displayClusters(
                                                     List<Set<String>> clusters, List<Double> correlations)
                                                     {
        // filterSmallClusters(clusters);
        MCLClusterResultDialog resultDialog = new MCLClusterResultDialog();
        resultDialog.setClusterResults(clusters, correlations);
        resultDialog.setModal(true);
        resultDialog.setVisible(true);
        if (resultDialog.isOkClicked)
            // Need to construct FI network
            return resultDialog.getSelectedClusters();
        else
            return null; // has been canceled
                                                     }
    
    private void filterSmallClusters(List<Set<String>> clusters)
    {
        
        for (Iterator<Set<String>> it = clusters.iterator(); it.hasNext();)
        {
            Set<String> cluster = it.next();
            if (cluster.size() < 3)
            {
                it.remove();
            }
        }
    }
    
    private List<Set<String>> parseClusterResults(Element resultElm)
            throws JDOMException {
        List<Set<String>> clusters = new ArrayList<Set<String>>();
        // // This is for test
        // String error = resultElm.getChildText("error");
        // System.out.println("Error output: \n" + error);
        List<?> children = resultElm.getChildren("clusters");
        for (Iterator<?> it = children.iterator(); it.hasNext();)
        {
            Element child = (Element) it.next();
            String text = child.getTextTrim();
            Set<String> cluster = new HashSet<String>();
            String[] tokens = text.split("\t");
            for (String token : tokens)
            {
                cluster.add(token);
            }
            clusters.add(cluster);
        }
        return clusters;
    }
    
    private List<Double> calculateAverageCorrelations(List<Set<String>> clusters, Set<String> fis,
                                                      Set<String> fisWithCorrs) {
        // Use this map for calculation
        Map<String, Double> fiToCorr = new HashMap<String, Double>();
        int index = 0;
        for (String fiWithCorr : fisWithCorrs)
        {
            index = fiWithCorr.lastIndexOf("\t");
            fiToCorr.put(fiWithCorr.substring(0, index), new Double(fiWithCorr
                                                                    .substring(index + 1)));
        }
        List<Double> corrs = new ArrayList<Double>();
        double total = 0.0d;
        int count = 0;
        index = 0;
        for (Set<String> cluster : clusters)
        {
            index++;
            Set<String> fisInCluster = InteractionUtilities
                    .getFIs(cluster, fis);
            total = 0.0d;
            count = 0;
            for (String fi : fisInCluster)
            {
                Double value = fiToCorr.get(fi);
                if (value != null)
                {
                    total += value;
                    count++;
                }
            }
            if (count == 0)
            {
                corrs.add(null);
            }
            else
            {
                corrs.add(total / count);
            }
        }
        return corrs;
   }
    
    
    private class MCLClusterResultDialog extends JDialog {
        private boolean isOkClicked;
        private JTable clusterTable;
        private JLabel numberLabel;
        private JLabel selectedClusterLabel;
        private JTextField sizeTF;
        private JTextField corrTF;
        
        public MCLClusterResultDialog()
        {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("MCL Clustering Results");
            // Top panel to show filters
            JPanel northPane = new JPanel();
            northPane.setLayout(new GridBagLayout());
            Border border = BorderFactory.createEmptyBorder(4, 4, 4, 4);
            northPane.setBorder(BorderFactory.createCompoundBorder(
                                                                   BorderFactory.createEtchedBorder(), border));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridwidth = 4;
            constraints.weightx = 0.5d;
            numberLabel = new JLabel(
                    "Total modules from clustering (size > 2): ");
            northPane.add(numberLabel, constraints);
            JLabel filterLabel = new JLabel(
                    "Choose filters (presss \"return\" to do filtering after entering numbers):");
            constraints.gridy = 1;
            northPane.add(filterLabel, constraints);
            JLabel sizeLabel = new JLabel("    Size (default 7): ");
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.weightx = 0.0d;
            northPane.add(sizeLabel, constraints);
            sizeTF = new JTextField();
            sizeTF.setColumns(6);
            constraints.gridx = 1;
            northPane.add(sizeTF, constraints);
            JLabel corrLabel = new JLabel("Average correlation (default 2.5): ");
            constraints.gridx = 2;
            northPane.add(corrLabel, constraints);
            corrTF = new JTextField();
            corrTF.setColumns(6);
            constraints.gridx = 3;
            northPane.add(corrTF, constraints);
            // Used to show the total numbers of selected clusters and genes in
            // clusters
            selectedClusterLabel = new JLabel("Total modules in table: ");
            constraints.gridwidth = 4;
            constraints.gridx = 0;
            constraints.gridy = 3;
            northPane.add(selectedClusterLabel, constraints);
            getContentPane().add(northPane, BorderLayout.NORTH);
            // central is for the table
            clusterTable = new JTable();
            final MCLClusterResultModel model = new MCLClusterResultModel();
            clusterTable.setModel(model);
            model.addTableModelListener(new TableModelListener()
            {
                
                @Override
                public void tableChanged(TableModelEvent e)
                {
                    int totalRow = clusterTable.getRowCount();
                    int totalGenes = model.getTotalGenesDisplayed();
                    selectedClusterLabel
                    .setText("Total modules in table: " + totalRow
                             + " (" + totalGenes + " genes in total)");
                }
            });
            getContentPane().add(new JScrollPane(clusterTable),
                                 BorderLayout.CENTER);
            // Control panel
            DialogControlPane controlPane = new DialogControlPane();
            JButton okBtn = controlPane.getOKBtn();
            okBtn.addActionListener(new ActionListener()
            {
                
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    dispose();
                    isOkClicked = true;
                }
            });
            // Don't enable default button. Return is used for filtering.
            // okBtn.setDefaultCapable(true);
            // getRootPane().setDefaultButton(okBtn);
            controlPane.getCancelBtn().addActionListener(new ActionListener()
            {
                
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    dispose();
                    isOkClicked = false;
                }
            });
            // Used to enable filtering
            ActionListener filterAction = new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    filterClusters();
                }
            };
            sizeTF.addActionListener(filterAction);
            corrTF.addActionListener(filterAction);
            // Add two default values we used
            sizeTF.setText("7");
            corrTF.setText("0.25");
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            setSize(670, 560);
            setLocationRelativeTo(getOwner());
        }
        
        public void setClusterResults(List<Set<String>> clusters,
                                      List<Double> correlations)
        {
            numberLabel.setText(numberLabel.getText() + "" + clusters.size());
            MCLClusterResultModel model = (MCLClusterResultModel) clusterTable
                    .getModel();
            model.setClusters(clusters, correlations);
            filterClusters();
        }
        
        private void filterClusters()
        {
            MCLClusterResultModel model = (MCLClusterResultModel) clusterTable
                    .getModel();
            String sizeText = sizeTF.getText().trim();
            Integer size = null;
            try
            {
                if (sizeText.length() > 0)
                {
                    size = new Integer(sizeText);
                }
                String corrText = corrTF.getText().trim();
                Double corr = null;
                if (corrText.length() > 0)
                {
                    corr = new Double(corrText);
                }
                model.filterAndDisplay(size, corr);
            }
            catch (NumberFormatException e)
            {
                JOptionPane.showMessageDialog(this,
                                              "Please make sure you enter numbers in the filters.",
                                              "Error in Filtering", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        public Map<Set<String>, Double> getSelectedClusters()
        {
            Map<Set<String>, Double> clusterToCorr = new HashMap<Set<String>, Double>();
            for (int i = 0; i < clusterTable.getRowCount(); i++)
            {
                String text = (String) clusterTable.getValueAt(i, clusterTable
                                                               .getColumnCount() - 1);
                String[] tokens = text.split(", ");
                Set<String> cluster = new HashSet<String>();
                for (String token : tokens)
                {
                    cluster.add(token);
                }
                String corrText = (String) clusterTable.getValueAt(i, 2);
                clusterToCorr.put(cluster, new Double(corrText));
            }
            return clusterToCorr;
        }
    }
    
    private class MCLClusterResultModel extends AbstractTableModel
    {
        private String[][] data;
        private String[] headers = new String[]
                { "Module Index", "Size", "Average Correlation", "Genes" };
        private List<Set<String>> clusters; // Cached for filtering purpose
        private List<Double> correlations;
        // cached value for fast return
        private int totalGenesDisplayed;
        
        public MCLClusterResultModel()
        {
        }
        
        public void setClusters(List<Set<String>> clusters,
                                List<Double> correlations)
        {
            this.clusters = clusters;
            this.correlations = correlations;
        }
        
        @Override
        public int getRowCount()
        {
            return data.length;
        }
        
        @Override
        public int getColumnCount()
        {
            return headers.length;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            return data[rowIndex][columnIndex];
        }
        
        @Override
        public String getColumnName(int column)
        {
            return headers[column];
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return String.class;
        }
        
        public void filterAndDisplay(Integer size, Double corr)
        {
            // Do a filtering
            // Create a map for doing filtering
            List<Set<String>> filtered = new ArrayList<Set<String>>();
            List<Double> filterCorrs = new ArrayList<Double>();
            for (int i = 0; i < clusters.size(); i++)
            {
                Set<String> cluster = clusters.get(i);
                Double value = correlations.get(i);
                if (size != null && size > cluster.size())
                {
                    continue;
                }
                if (value == null || (corr != null && corr > value))
                {
                    continue;
                }
                filtered.add(cluster);
                filterCorrs.add(value);
            }
            data = new String[filtered.size()][];
            totalGenesDisplayed = 0;
            for (int i = 0; i < filtered.size(); i++)
            {
                Set<String> cluster = filtered.get(i);
                data[i] = new String[5];
                data[i][0] = i + "";
                data[i][1] = cluster.size() + "";
                // Format the value a little bit
                data[i][2] = String.format("%.4f", filterCorrs.get(i));
                data[i][3] = InteractionUtilities.joinStringElements(", ",
                                                                     cluster);
                totalGenesDisplayed += cluster.size();
            }
            fireTableDataChanged();
        }
        
        public int getTotalGenesDisplayed()
        {
            return totalGenesDisplayed;
        }
        
    }
}
