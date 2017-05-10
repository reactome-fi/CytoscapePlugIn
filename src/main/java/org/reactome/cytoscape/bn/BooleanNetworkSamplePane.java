/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.swing.CytoPanel;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.SelectionMediator;
import org.gk.render.Node;
import org.reactome.booleannetwork.Attractor;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.booleannetwork.FuzzyLogicSimulator;
import org.reactome.booleannetwork.FuzzyLogicSimulator.ANDGateMode;
import org.reactome.booleannetwork.SimulationConfiguration;
import org.reactome.cytoscape.bn.SimulationTableModel.EntityType;
import org.reactome.cytoscape.bn.SimulationTableModel.ModificationType;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

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
    // To synchronize selection
    private VariableSelectionHandler selectionHandler;
    // For simulation
    private Double defaultValue = 1.0d; // Default is on
    // Simulation name
    private String sampleName;
    // For simulation
    private ANDGateMode andGateMode = ANDGateMode.PROD;
    // A checkbox for selecting if a larger value should be used for stimulation nodes
    private JCheckBox useLargerValueBox;
    // Extra information
    private Map<String, Double> proteinInhibtion;
    private Map<String, Double> proteinActivation;
   
    /**
     * Default constructor.
     */
    public BooleanNetworkSamplePane() {
        init();
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
        
        add(new JScrollPane(sampleTable), BorderLayout.CENTER);
        
        useLargerValueBox = new JCheckBox("Use larger values for stimulation variables");
        useLargerValueBox.setSelected(true); // Default is true
        add(useLargerValueBox, BorderLayout.SOUTH);
        
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
        
        return table;
    }
    
    private void enableSelectionSync() {
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
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        
        getParent().remove(this);
    }
    
    private void handleTableSelection() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
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

    public void setBooleanNetwork(BooleanNetwork network) {
        this.network = network;
        SimulationTableModel model = (SimulationTableModel) sampleTable.getModel();
        model.setBooleanNetwork(network,
                                getDisplayedIds(),
                                defaultValue,
                                proteinInhibtion,
                                proteinActivation);
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
                                           "New Simulation",
                                           JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SimulationConfiguration configuration = model.getConfiguration();
        
        mergeWithOtherConfig(configuration);
        
        FuzzyLogicSimulator simulator = new FuzzyLogicSimulator();
        simulator.setAndGateMode(this.andGateMode);
        simulator.simulate(network, configuration);
        
        if (!simulator.isAttractorReached()) {
            JOptionPane.showMessageDialog(this,
                                          "Attractor cannot be reached!",
                                          "No Attractor",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Attractor attractor = simulator.getAttractor();
        model.addAttractor(attractor);
        
        displayTimeCourse(model.getVariables());
    }

    protected void mergeWithOtherConfig(SimulationConfiguration configuration) {
        // Don't forget this choice
        configuration.setUseLargerValueForStimulation(useLargerValueBox.isSelected());
        // There are other variables that need values
        // Make sure all variables have values
        Map<BooleanVariable, Number> varToValue = configuration.getInitial();
        for (BooleanVariable var : network.getVariables()) {
            if (!varToValue.containsKey(var))
                varToValue.put(var, PlugInUtilities.getBooleanDefaultValue(var, defaultValue));
        }
        if (proteinInhibtion != null) {
            Map<BooleanVariable, Double> inhibition = configuration.getInhibition();
            mergeProteinModification(inhibition, proteinInhibtion);
        }
        if (proteinActivation != null) {
            Map<BooleanVariable, Double> activation = configuration.getActivation();
            mergeProteinModification(activation, proteinActivation);
        }
    }

    protected void mergeProteinModification(Map<BooleanVariable, Double> inhibition,
                                            Map<String, Double> proteinInhibtion) {
        for (BooleanVariable var : network.getVariables()) {
            if (inhibition.containsKey(var))
                continue; // Always use the current setting
            // Only merge into variables without inputs
            if (var.getInRelations() != null && var.getInRelations().size() > 0)
                continue;
            String gene = (String) var.getProperty("gene");
            if (gene == null)
                continue;
            Double preInhibit = proteinInhibtion.get(gene);
            if (preInhibit != null)
                inhibition.put(var, preInhibit);
        }
    }
    
    private void displayTimeCourse(List<BooleanVariable> variables) {
        TimeCoursePane timeCoursePane = new TimeCoursePane("BN: " + sampleName);
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(timeCoursePane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(timeCoursePane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        // The following order is important to highlight pathway diagram..
        timeCoursePane.setHiliteControlPane(hiliteControlPane);
        timeCoursePane.setSimulationResults(variables);
    }
}
