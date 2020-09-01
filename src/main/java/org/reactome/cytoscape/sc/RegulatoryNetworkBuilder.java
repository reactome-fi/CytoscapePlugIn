package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.sc.RegulatoryNetworkStyle.CORRELATION_COL_NAME;
import static org.reactome.cytoscape.sc.RegulatoryNetworkStyle.CORRELATION_PVALUE_COL_NAME;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.gk.util.DialogControlPane;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.funcInt.FIAnnotation;

/**
 * This class is used to build a regulatory network based on the dorothean TF/Target interactions.
 * @author wug
 *
 */
public class RegulatoryNetworkBuilder {
    private JSONServerCaller serverCaller;

    public RegulatoryNetworkBuilder() {
    }
    
    public void setServerCaller(JSONServerCaller caller) {
        this.serverCaller = caller;
    }
    
    public void build() {
        try {
            List<String> cellTimeKeys = serverCaller.getCellTimeKeys();
            if (cellTimeKeys == null || cellTimeKeys.size() == 0) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find any information related to cell time.\n" +
                                              "Please run diffusion pseudotime analysis first.",
                                              "No Cell Time",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<Integer> clusters = serverCaller.getCluster();
            clusters = clusters.stream().distinct().sorted().collect(Collectors.toList());
            ConfigDialog dialog = new ConfigDialog();
            dialog.setCellClusters(clusters);
            dialog.setCellTimeKeys(cellTimeKeys);
            dialog.setModal(true);
            dialog.setVisible(true);
            if (!dialog.isOkClicked)
                return;
            Thread t = new Thread() {
                public void run() {
                    buildNetwork(dialog);
                }
            };
            t.start();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in building gene regulatory network: \n" + e.getMessage(),
                                          "Error in Building Regulatory Network",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void buildNetwork(ConfigDialog dialog) {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            ProgressPane progressPane = new ProgressPane();
            progressPane.setIndeterminate(true);
            frame.setGlassPane(progressPane);
            progressPane.setTitle("Build Regulatory Network");
            progressPane.setVisible(true);
            // Load tf/target interactions
            progressPane.setText("Fetching TF/Target interactions...");
            RESTFulFIService fiService = new RESTFulFIService();
            List<String> interactions = fiService.queryDorotheaFIs(ScNetworkManager.getManager().getSpecies());
            // Calculate correlations
            progressPane.setText("Calculating correlations...");
            String layer = ScNetworkManager.getManager().isForRNAVelocity() ? "velocity" : "null";
            Map<String, List<Double>> fiToCor = serverCaller.calculateGeneRelationships(interactions,
                                                                                        dialog.getSelectedCellGroups(), 
                                                                                        dialog.getCellTimeKey(),
                                                                                        layer,
                                                                                        dialog.getTimeDelay(),
                                                                                        dialog.getCorMethod());
            buildNetwork(fiToCor,
                         dialog.getValueCutoff(), 
                         dialog.getPValueCutoff(),
                         dialog.getSelectedCellGroups(),
                         fiService, 
                         progressPane);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in building gene regulatory network: \n" + e.getMessage(),
                                          "Error in Building Regulatory Network",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        frame.getGlassPane().setVisible(false);
    }
    
    private void buildNetwork(Map<String, List<Double>> fiToCor,
                              double valueCutoff,
                              double pvalueCutoff,
                              List<String> groups,
                              RESTFulFIService fiService,
                              ProgressPane progressPane) throws Exception {
        progressPane.setText("Building network...");
        Set<String> fis = fiToCor.keySet()
                .stream()
                .filter(fi -> {
                    List<Double> cor = fiToCor.get(fi);
                    return Math.abs(cor.get(0)) >= valueCutoff && cor.get(1) <= pvalueCutoff;
                })
                .collect(Collectors.toSet());
        // For edge annotations
        fiToCor.keySet().retainAll(fis);
        fis = mappedToHuman(fis); // Mouse FIs should be mapped to human in our display for functional analysis
        // Build CyNetwork
        FINetworkGenerator generator = new FINetworkGenerator();
        generator.setDirectionInEdgeName(true);
        Set<String> nodes = fis.stream()
                               .map(fi -> fi.split("\t"))
                               .flatMap(genes -> Stream.of(genes))
                               .collect(Collectors.toSet());
        CyNetwork network = generator.constructFINetwork(nodes, fis);
        PlugInObjectManager manager = PlugInObjectManager.getManager();
        CyNetworkManager networkManager = manager.getNetworkManager();
        networkManager.addNetwork(network);

        // Handle network related properties. Do this at the end to avoid null exception because of no view available.
        TableHelper tableHelper = new TableHelper();
        tableHelper.storeFINetworkVersion(network, PlugInObjectManager.getManager().getFiNetworkVersion());
        tableHelper.markAsReactomeNetwork(network, 
                                          ReactomeNetworkType.DorotheaTFTargetNetwork);
        tableHelper.storeNetworkAttribute(network, "name", 
                                          "Regulatory network for groups: " + String.join(", ", groups));
        mapMouseGenesToHuman(nodes, tableHelper, network);
        checkIfTFs(nodes, fis, tableHelper, network);

        // Need to annotate the network
        progressPane.setText("Annotating network...");
        annotateInteractions(fis, fiService, tableHelper, network);
        
        storeCors(fiToCor, tableHelper, network);
        
        progressPane.setText("Creating network view...");
        // Do this at the end after we have set up tables
        // Build network view
        CyNetworkViewFactory viewFactory = manager.getNetworkViewFactory();
        CyNetworkView view = viewFactory.createNetworkView(network);
        CyNetworkViewManager viewManager = manager.getNetworkViewManager();
        viewManager.addNetworkView(view);
        
        RegulatoryNetworkStyle visStyle = new RegulatoryNetworkStyle();
        visStyle.setVisualStyle(view);
        visStyle.doLayout();
        view.updateView();
    }
    
    private void annotateInteractions(Set<String> fis,
                                      RESTFulFIService service,
                                      TableHelper tableHelper,
                                      CyNetwork network) throws Exception {
        // Since we have converted mouse FIs to Human FIs if it is mouse, always use human for this query.
        Map<String, FIAnnotation> edgeIdToAnnotation = service.annotateDorotheaFIs(fis,
                                                                                   PathwaySpecies.Homo_sapiens);
        Map<String, String> edgeToAnnotation = new HashMap<>();
        Map<String, String> edgeToDirection = new HashMap<>();
        for (String edgeId : edgeIdToAnnotation.keySet()) {
            FIAnnotation annotation = edgeIdToAnnotation.get(edgeId);
            edgeId = edgeId.replace("\t", " (FI) ");
            edgeToAnnotation.put(edgeId, annotation.getAnnotation());
            edgeToDirection.put(edgeId, annotation.getDirection());
        }
        tableHelper.storeEdgeAttributesByName(network, "FI Annotation", edgeToAnnotation);
        tableHelper.storeEdgeAttributesByName(network, "FI Direction", edgeToDirection);
    }
    
    private void checkIfTFs(Set<String> nodes,
                            Set<String> fis,
                            TableHelper tableHelper,
                            CyNetwork network) {
        Set<String> tfs = fis.stream().map(fi -> fi.split("\t")[0]).collect(Collectors.toSet());
        Map<String, Boolean> nodeToValue = nodes.stream().collect(Collectors.toMap(Function.identity(),
                                                                                   node -> tfs.contains(node)));
        tableHelper.storeNodeAttributesByName(network, RegulatoryNetworkStyle.TF_COL_NAME, nodeToValue);
    }
    
    private void storeCors(Map<String, List<Double>> fiToCor,
                           TableHelper tableHelper,
                           CyNetwork network) throws Exception {
        Map<String, Double> fiToValue = new HashMap<>();
        Map<String, Double> fiToPValue = new HashMap<>();
        Map<String, Set<String>> mouse2humanMap = null;
        if (ScNetworkManager.getManager().getSpecies() == PathwaySpecies.Mus_musculus)
            mouse2humanMap = ScNetworkManager.getManager().getMouse2humanGeneMap();
        for (String fi : fiToCor.keySet()) {
            List<Double> cor = fiToCor.get(fi);
            if (mouse2humanMap == null) {
                fi = fi.replace("\t", " (FI) ");
                fiToValue.put(fi, cor.get(0));
                fiToPValue.put(fi, cor.get(1));
            }
            else {
                String[] genes = fi.split("\t");
                Set<String> mapped1 = mouse2humanMap.get(genes[0]);
                Set<String> mapped2 = mouse2humanMap.get(genes[1]);
                if (mapped1 == null || mapped2 == null)
                    continue;
                for (String m1 : mapped1) 
                    for (String m2 : mapped2) {
                        fiToValue.put(m1 + " (FI) " + m2, cor.get(0));
                        fiToPValue.put(m1 + " (FI) " + m2, cor.get(1));
                    }
            }
        }
        tableHelper.storeEdgeAttributesByName(network, CORRELATION_COL_NAME, fiToValue);
        tableHelper.storeEdgeAttributesByName(network, CORRELATION_PVALUE_COL_NAME, fiToPValue);
    }
    
    private Set<String> mappedToHuman(Set<String> fis) throws Exception {
        if (ScNetworkManager.getManager().getSpecies() == PathwaySpecies.Homo_sapiens)
            return fis;
        Set<String> rtn = new HashSet<>();
        Map<String, Set<String>> mouse2humanMap = ScNetworkManager.getManager().getMouse2humanGeneMap();
        for (String fi : fis) {
            String[] genes = fi.split("\t");
            Set<String> mapped1 = mouse2humanMap.get(genes[0]);
            Set<String> mapped2 = mouse2humanMap.get(genes[1]);
            if (mapped1 == null || mapped2 == null)
                continue;
            for (String m1 : mapped1) 
                for (String m2 : mapped2)
                    rtn.add(m1 + "\t" + m2);
        }
        return rtn;
    }
    
    private void mapMouseGenesToHuman(Set<String> genes, 
                                      TableHelper helper,
                                      CyNetwork network) throws Exception {
        if (ScNetworkManager.getManager().getSpecies() == PathwaySpecies.Homo_sapiens)
            return;
        Map<String, Set<String>> mouse2humanMap = ScNetworkManager.getManager().getMouse2humanGeneMap();
        helper.storeMouse2HumanGeneMap(mouse2humanMap, genes, network);
    }
    
    public static void main(String[] args) {
        ConfigDialog dialog = new ConfigDialog();
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                System.out.println("Window Size: " + dialog.getSize());
            }
            
        });
        dialog.setVisible(true);
    }
    
    @SuppressWarnings("serial")
    private static class ConfigDialog extends JDialog {
        private boolean isOkClicked;
        private JButton okBtn;
        private JList<String> groupList;
        private JComboBox<String> corBox;
        private JComboBox<String> timeBox;
        private JSpinner timeSpinner;
        private JTextField valueTF;
        private JTextField pvalueTF;
        // Not supported for the time being to avoid confusion
//        private JComboBox<String> layerBox;
        
        public ConfigDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        public void setCellClusters(List<Integer> clusters) {
            DefaultListModel<String> model = (DefaultListModel<String>) groupList.getModel();
            model.removeAllElements();
            model.addElement("all");
            clusters.forEach(c -> model.addElement(c + ""));
            groupList.setSelectedIndex(0);
        }
        
        public void setCellTimeKeys(List<String> keys) {
            timeBox.removeAllItems();
            keys.forEach(k -> timeBox.addItem(k));
            timeBox.setSelectedIndex(0);
        }
        
        public List<String> getSelectedCellGroups() {
            return groupList.getSelectedValuesList();
        }
        
        public String getCorMethod() {
            return corBox.getSelectedItem().toString().toLowerCase();
        }
        
        public String getCellTimeKey() {
            return timeBox.getSelectedItem().toString();
        }
        
        public int getTimeDelay() {
            return (Integer) timeSpinner.getModel().getValue();
        }
        
        public double getValueCutoff() {
            return Double.parseDouble(valueTF.getText().trim());
        }
        
        public double getPValueCutoff() {
            return Double.parseDouble(pvalueTF.getText().trim());
        }
        
        private void init() {
            setTitle("Regulatory Network Configuration");
            
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            
            // Correlation method
            JLabel label = new JLabel("Choose correlation method:");
            corBox = new JComboBox<String>();
            corBox.addItem("Spearman");
            corBox.addItem("Pearson");
            corBox.addItem("Kendall");
            corBox.setSelectedIndex(0);
            constraints.gridx = 0;
            constraints.gridy = 0;
            contentPane.add(label, constraints);
            constraints.gridx ++;
            contentPane.add(corBox, constraints);
            
            // Correlation thresholds
            label = new JLabel("Select TF/Target interactions with correlations:");
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            JPanel filterPane = createFilterPane();
            constraints.gridy ++;
            contentPane.add(filterPane, constraints);
            
            // Cell data and correlation
            label = new JLabel("Choose cell time type:");
            constraints.gridx = 0;
            constraints.gridy ++;
            contentPane.add(label, constraints);
            timeBox = new JComboBox<String>();
            timeBox.addItem("latent_time");
            timeBox.addItem("dpt_pseudotime");
            constraints.gridx ++;
            contentPane.add(timeBox, constraints);
            
            // Time delay window
            label = new JLabel("Choose time delay:");
            constraints.gridx = 0;
            constraints.gridy ++;
            contentPane.add(label, constraints);
            SpinnerModel timeModel = new SpinnerNumberModel(7, 4, 10, 1);
            timeSpinner = new JSpinner(timeModel);
            constraints.gridx ++;
            contentPane.add(timeSpinner, constraints);
            
            setCellGroupGUIs(contentPane, constraints);
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(e -> {
                if (!ensureValues())
                    return;
                isOkClicked = true;
                dispose();
            });
            controlPane.getCancelBtn().addActionListener(e -> {
                isOkClicked = false;
                dispose();
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            this.okBtn = controlPane.getOKBtn();
            
            setSize(530, 410);
            setLocationRelativeTo(getOwner());
        }
        
        private boolean ensureValues() {
            try {
                Double.parseDouble(valueTF.getText().trim());
                Double.parseDouble(pvalueTF.getText().trim());
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Text for correlation or p-value selection is not a number.",
                                              "Error in Configration",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        private void setCellGroupGUIs(JPanel contentPane, GridBagConstraints constraints) {
            JLabel label = new JLabel("Choose cell groups:");
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            groupList = new JList<>();
            groupList.setToolTipText("Hold the command/control key for multi-selection and de-selection");
            DefaultListModel<String> listModel = new DefaultListModel<>();
            listModel.addElement("all");
            IntStream.range(0, 30).forEach(i -> listModel.addElement(i + ""));
            groupList.setModel(listModel);
            groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            int visibleRow = 5;
            groupList.setVisibleRowCount(visibleRow);
            groupList.setSelectedIndex(0); // The default should be all
            constraints.gridx ++;
            constraints.gridheight = 3;
            contentPane.add(new JScrollPane(groupList), constraints);
            // Show selected cell groups
            label = new JLabel("Selected cell groups:");
            constraints.gridx = 0;
            constraints.gridy += visibleRow;
            constraints.gridheight = 1;
            contentPane.add(label, constraints);
            JLabel selectedGroupLabel = new JLabel("all");
            constraints.gridx ++;
            contentPane.add(selectedGroupLabel, constraints);
            
            groupList.getSelectionModel().addListSelectionListener(e -> {
                List<String> selected = groupList.getSelectedValuesList();
                if (selected.contains("all"))
                    selectedGroupLabel.setText("all");
                else
                    selectedGroupLabel.setText(String.join(", ", selected));
                okBtn.setEnabled(selected.size() > 0);
            });
        }
        
        private JPanel createFilterPane() {
            JPanel pane = new JPanel();
            pane.setLayout(new FlowLayout(FlowLayout.CENTER, 4,  4));
            JLabel valueLabel = new JLabel("Value >= or <=");
            valueTF = new JTextField("0.01");
            valueTF.setColumns(4);
            JLabel pValueLabel = new JLabel("and p-Value <=");
            pvalueTF = new JTextField("0.05");
            pvalueTF.setColumns(4);
            pane.add(valueLabel);
            pane.add(valueTF);
            pane.add(pValueLabel);
            pane.add(pvalueTF);
            return pane;
        }
        
    }
    
    
}
