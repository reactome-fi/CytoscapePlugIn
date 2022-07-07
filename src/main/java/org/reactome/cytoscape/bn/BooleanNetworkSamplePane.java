/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.swing.CytoPanel;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.SelectionMediator;
import org.gk.render.Node;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.booleannetwork.FuzzyLogicSimulator;
import org.reactome.booleannetwork.FuzzyLogicSimulator.ANDGateMode;
import org.reactome.booleannetwork.SimulationConfiguration;
import org.reactome.booleannetwork.SimulationResults;
import org.reactome.booleannetwork.TransferFunction;
import org.reactome.cytoscape.bn.SimulationTableModel.EntityType;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pathway.booleannetwork.BNPerturbationAnalyzer;
import org.reactome.pathway.booleannetwork.BNPerturbationInjector;
import org.reactome.pathway.booleannetwork.ModificationType;

/**
 * This customized JPanel is used to set up initial values and then list simulation results.
 * network modeling.
 * @author gwu
 *
 */
public class BooleanNetworkSamplePane extends JPanel {
    private BooleanNetwork network;
    private PathwayEditor pathwayEditor;
    private PathwayHighlightControlPanel hiliteControlPane;
    private JTable sampleTable;
    private JTextArea noteTF;
    // To synchronize selection
    private VariableSelectionHandler selectionHandler;
    // For simulation
    private Double defaultValue = 1.0d; // Default is on
    // Simulation name
    protected String sampleName;
    // For simulation
    private ANDGateMode andGateMode = ANDGateMode.PROD;
    // A checkbox for selecting if a larger value should be used for stimulation nodes
    private JCheckBox useLargerValueBox;
    // Extra information
    private Map<String, Double> proteinInhibtion;
    private Map<String, Double> proteinActivation;
    // Transfer function
    private TransferFunction transferFunction;
   
    /**
     * Default constructor.
     */
    public BooleanNetworkSamplePane() {
        init();
    }

    public TransferFunction getTransferFunction() {
        return transferFunction;
    }

    public void setTransferFunction(TransferFunction transferFunction) {
        this.transferFunction = transferFunction;
    }

    public void setProteinInhibtion(Map<String, Double> preInhibtion) {
        this.proteinInhibtion = preInhibtion;
    }

    public void setProteinActivation(Map<String, Double> preActivation) {
        this.proteinActivation = preActivation;
    }

    public ANDGateMode getAndGateMode() {
        return andGateMode;
    }

    public void setAndGateMode(ANDGateMode andGateMode) {
        this.andGateMode = andGateMode;
    }

    public String getSampleName() {
        return sampleName;
    }
    
    public SimulationTableModel getSimulation() {
        return (SimulationTableModel) sampleTable.getModel();
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
        SimulationTableModel model = (SimulationTableModel) sampleTable.getModel();
        model.setSimulationName(sampleName);
    }

    public Double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Double defaultValue) {
        this.defaultValue = defaultValue;
    }

    private void init() {
        setLayout(new BorderLayout());
        
        sampleTable = createSampleTable();
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        
        contentPane.add(new JScrollPane(sampleTable), BorderLayout.CENTER);
        
        useLargerValueBox = new JCheckBox("Use larger values for stimulation variables");
        useLargerValueBox.setSelected(true); // Default is true
        contentPane.add(useLargerValueBox, BorderLayout.SOUTH);
        
        add(contentPane, BorderLayout.CENTER);
        
        noteTF = PlugInUtilities.createTextAreaForNote(this);
        add(noteTF, BorderLayout.SOUTH);
        
        enableSelectionSync();
    }
    
    private JTable createSampleTable() {
        JTable table = new JTable();
        table.setDefaultRenderer(BooleanVariable.class, new DefaultTableCellRenderer() {

            @Override
            protected void setValue(Object value) {
                BooleanVariable var = (BooleanVariable) value;
                setText(var == null ? "" : var.getName());
            }
            
        });
        SimulationTableModel model = new SimulationTableModel();
        table.setModel(model);
        TableRowSorter<SimulationTableModel> sorter = new TableRowSorter<SimulationTableModel>(model);
        sorter.setComparator(0, new Comparator<BooleanVariable>() {
            public int compare(BooleanVariable var1, BooleanVariable var2) {
                return var1.getName().compareTo(var2.getName());
            }
        });
        sorter.setComparator(1, new Comparator<EntityType>() {
            public int compare(EntityType type1, EntityType type2) {
                return type1.toString().compareTo(type2.toString());
            }
        });
        table.setRowSorter(sorter);
        
        DefaultTableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer();
        
        TableColumn typeColoumn = table.getColumnModel().getColumn(1);
        JComboBox<EntityType> typeEditor = new JComboBox<>();
        for (EntityType type : EntityType.values())
            typeEditor.addItem(type);
        typeColoumn.setCellEditor(new DefaultCellEditor(typeEditor));
        typeColoumn.setCellRenderer(defaultCellRenderer);
        
        TableColumn modificationCol = table.getColumnModel().getColumn(3);
        JComboBox<ModificationType> modificationEditor = new JComboBox<>();
        for (ModificationType type : ModificationType.values())
            modificationEditor.addItem(type);
        modificationCol.setCellEditor(new DefaultCellEditor(modificationEditor));
        modificationCol.setCellRenderer(defaultCellRenderer);
        
        DefaultCellEditor numberEditor = new DefaultCellEditor(new JTextField()) {
            @Override
            public Object getCellEditorValue() {
                String text = super.getCellEditorValue().toString();
                return new Double(text);
            }
        };
        
        TableColumn initCol = table.getColumnModel().getColumn(2);
        initCol.setCellEditor(numberEditor);
        initCol.setCellRenderer(defaultCellRenderer);
        
        TableColumn modificationValueCol = table.getColumnModel().getColumn(4);
        modificationValueCol.setCellEditor(numberEditor);
        modificationValueCol.setCellRenderer(defaultCellRenderer);
        
        // Add a popup menu for batch editing 
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doTablePopup(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doTablePopup(e.getX(), e.getY());
            }
        
        });
        
        return table;
    }
    
    private void doTablePopup(int x, int y) {
        // At least two variables are selected
        if (isSimulationPerformed() || sampleTable.getSelectedRowCount() < 2)
            return; // Cannot make change
        JPopupMenu popup = new JPopupMenu();
        JMenuItem batchEdit = new JMenuItem("Edit in Batch");
        popup.add(batchEdit);
        batchEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editTableInBatch();
            }
        });
        popup.show(sampleTable,
                   x,
                   y);
    }
    
    private void editTableInBatch() {
        BatchEditDialog dialog = new BatchEditDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        // Assign values
        EntityType type = (EntityType) dialog.typeBox.getSelectedItem();
        Double initial = new Double(dialog.initialBox.getText().trim());
        ModificationType modification = (ModificationType) dialog.modificationBox.getSelectedItem();
        Double strentgh = new Double(dialog.strengthBox.getText().trim());
        TableModel model = sampleTable.getModel();
        for (int rowIndex : sampleTable.getSelectedRows()) {
            int modelIndex = sampleTable.convertRowIndexToModel(rowIndex);
            model.setValueAt(type, modelIndex, 1);
            model.setValueAt(initial, modelIndex, 2);
            model.setValueAt(modification, modelIndex, 3);
            model.setValueAt(strentgh, modelIndex, 4);
        }
    }
    
    protected void enableSelectionSync() {
        sampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleTableSelection();
            }
        });
        
        selectionHandler = new VariableSelectionHandler();
        selectionHandler.setVariableTable(sampleTable);
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
    }
    
    public void delete() {
        // Need a confirmation
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "Are you sure you want to delete the selected simulation?",
                                                  "Delete?",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.WARNING_MESSAGE);
        if (reply != JOptionPane.OK_OPTION)
            return;
        remove();
    }

    public void remove() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        
        getParent().remove(this);
    }
    
    private void handleTableSelection() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    public Long getPathwayDiagramID() {
        if (pathwayEditor == null || pathwayEditor.getRenderable() == null)
            return null;
        RenderablePathway pathway = (RenderablePathway) pathwayEditor.getRenderable();
        return pathway.getReactomeDiagramId();
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    public void setBooleanNetwork(BooleanNetwork network,
                                  String drugs) {
        this.network = network;
        SimulationTableModel model = (SimulationTableModel) sampleTable.getModel();
        model.setBooleanNetwork(network,
                                getDisplayedIds(),
                                defaultValue,
                                proteinInhibtion,
                                proteinActivation);
        showNotes(network, drugs);
    }
    
    public BooleanNetwork getBooleanNetwork() {
    	return this.network;
    }
    
    private void showNotes(BooleanNetwork network, String drugs) {
        StringBuilder builder = new StringBuilder();
        builder.append("*: Simuation for ");
        builder.append(network.getName());
        if (transferFunction != null) 
            builder.append(" using ").append(transferFunction);
        if (drugs != null)
            builder.append(" (").append(drugs + ")");
        builder.append(".");
        noteTF.setText(builder.toString());
    }
    
    private Set<String> getDisplayedIds() {
        Set<String> ids = new HashSet<>();
        if (pathwayEditor != null) {
            for (Object o : pathwayEditor.getRenderable().getComponents()) {
                if (o instanceof Node) {
                    Node node = (Node) o;
                    if (node.getReactomeId() != null)
                        ids.add(node.getReactomeId().toString());
                }
            }
        }
        return ids;
    }
    
    public boolean isSimulationPerformed() {
        SimulationTableModel model = (SimulationTableModel) sampleTable.getModel();
        return model.isSimulationPerformed();
    }
    
    public void simulate() {
        SimulationTableModel model = (SimulationTableModel) sampleTable.getModel();
        if (model.isSimulationPerformed()) {
            JOptionPane.showMessageDialog(this,
                                          "Simulation has been performed for this sample.\n" + 
                                           "Create another sample for new simulation.",
                                           "New Logic Model Simulation",
                                           JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SimulationConfiguration configuration = model.getConfiguration();
        
        mergeWithOtherConfig(configuration);
        
        FuzzyLogicSimulator simulator = new FuzzyLogicSimulator();
        simulator.setTransferFunction(getTransferFunction());
        simulator.setAndGateMode(this.andGateMode);
        simulator.simulate(network, configuration);
        
        if (!simulator.isAttractorReached()) {
            JOptionPane.showMessageDialog(this,
                                          "Attractor cannot be reached!",
                                          "No Attractor",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        SimulationResults results = new SimulationResults();
        results.recordResults(network, simulator);
        
        model.setSimulationResults(results);
        // As of August 1, turn off attractor display here to avoid
        // clutter. The user should be able to find attractors in the
        // time course plot.
//        model.addAttractor(results.getAttractor());
        
        displayTimeCourse(model.getVariables());
    }

    protected void mergeWithOtherConfig(SimulationConfiguration configuration) {
        // Don't forget this choice
        configuration.setUseLargerValueForStimulation(useLargerValueBox.isSelected());
        // There are other variables that need values
        // Make sure all variables have values
        Map<BooleanVariable, Number> varToValue = configuration.getInitial();
        BNPerturbationAnalyzer helper = new BNPerturbationAnalyzer();
        Map<BooleanVariable, Number> otherVarToInit = helper.createInitials(network, 
                                                                            defaultValue);
        for (BooleanVariable var : network.getVariables()) {
            if (!varToValue.containsKey(var)) {
                Number init = otherVarToInit.get(var);
                varToValue.put(var, init);
            }
        }
        BNPerturbationInjector injector = new BNPerturbationInjector();
        injector.inject(network,
                proteinInhibtion,
                proteinActivation,
                configuration.getInhibition(),
                configuration.getActivation());
    }

    protected void displayTimeCourse(List<BooleanVariable> variables) {
        TimeCoursePane timeCoursePane = new TimeCoursePane("BN: " + sampleName);
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(timeCoursePane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(timeCoursePane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        // The following order is important to highlight pathway diagram..
        timeCoursePane.setHiliteControlPane(hiliteControlPane);
        timeCoursePane.setSimulationResults(variables);
    }
    
    /**
     * A customized JDialog for batch editing.
     * @author gwu
     *
     */
    private class BatchEditDialog extends JDialog {
        private boolean isOkClicked = false;
        private JButton okBtn;
        private JComboBox<EntityType> typeBox;
        private JTextField initialBox;
        private JComboBox<ModificationType> modificationBox;
        private JTextField strengthBox;
        
        public BatchEditDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Configuring Variables");
            
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEtchedBorder());
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 2, 2, 2);
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Configure selected variables:");
            constraints.gridwidth = 2;
            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(label, constraints);
            constraints.gridwidth = 1;
            
            typeBox = new JComboBox<>(EntityType.values());
            typeBox.setSelectedItem(EntityType.Respondent); // Default
            addRow("Type", typeBox, panel, constraints);
            initialBox = new JTextField();
            // Default value
            initialBox.setText("0.0");
            initialBox.setColumns(6);
            addRow("Initial", initialBox, panel, constraints);
            modificationBox = new JComboBox<>(ModificationType.values());
            modificationBox.setSelectedItem(ModificationType.None); // Default
            addRow("Modification", modificationBox, panel, constraints);
            strengthBox = new JTextField();
            strengthBox.setColumns(6);
            strengthBox.setText("0.0");
            addRow("Strength", strengthBox, panel, constraints);
            getContentPane().add(panel, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!validateValues())
                        return;
                    isOkClicked = true;
                    dispose();
                }
            });
            this.okBtn = controlPane.getOKBtn();
            this.okBtn.setEnabled(false);
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(400, 260);
            setLocationRelativeTo(getOwner());
            
            // Need to hook up listeners at the end to avoid null exception
            ActionListener cbListener = new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    okBtn.setEnabled(true);
                }
            };
            
            DocumentListener docListnerer = new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    okBtn.setEnabled(true);
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    okBtn.setEnabled(true);
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            };

            initialBox.getDocument().addDocumentListener(docListnerer);
            strengthBox.getDocument().addDocumentListener(docListnerer);
            typeBox.addActionListener(cbListener);
            modificationBox.addActionListener(cbListener);
        }
        
        private boolean validateValues() {
            String initial = initialBox.getText().trim();
            try {
                Double value = new Double(initial);
                // This is a hack
                if (value < 0.0 || value > 1.0)
                    throw new NumberFormatException();
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Error in Initial",
                                              "Initial should be a number between 0 and 1.0 (inclusive)!",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String strenth = strengthBox.getText().trim();
            try {
                Double value = new Double(strenth);
                // This is a hack
                if (value < 0.0 || value > 1.0)
                    throw new NumberFormatException();
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Error in Strength",
                                              "Strength should be a number between 0 and 1.0 (inclusive)!",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        private void addRow(String text, 
                            JComponent comp, 
                            JPanel panel,
                            GridBagConstraints constraints) {
            JLabel label = new JLabel(text + ": ");
            constraints.gridy ++;
            constraints.gridx = 0;
            panel.add(label, constraints);
            constraints.gridx = 1;
            panel.add(comp, constraints);
        }
        
    }
}
