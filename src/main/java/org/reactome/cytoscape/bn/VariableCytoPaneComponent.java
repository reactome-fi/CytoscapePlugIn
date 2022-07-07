/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.gk.graphEditor.SelectionMediator;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.bn.SimulationTableModel.EntityType;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * @author gwu
 *
 */
public abstract class VariableCytoPaneComponent extends NetworkModulePanel {
    protected static final String BUTTON_GROUP_NAME = "HighlightPathway";
    // For selection diagram entities
    private VariableSelectionHandler selectionHandler;
    // For highlight pathway diagrams
    protected PathwayHighlightControlPanel hiliteControlPane;
    // To control highlight manually
    protected JRadioButton hiliteDiagramBtn;
    
    public VariableCytoPaneComponent(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        modifyContentPane();
        synchronizeSelection();
    }
    
    protected void modifyContentPane() {
        // For showing BooleanVariable
        contentTable.setDefaultRenderer(BooleanVariable.class, new DefaultTableCellRenderer() {

            @Override
            protected void setValue(Object value) {
                BooleanVariable var = (BooleanVariable) value;
                setText(var == null ? "" : var.getName());
            }
            
        });
        // Disable this to avoid any issue
        contentTable.getTableHeader().setReorderingAllowed(false);
    }
    
    protected void createHighlightViewBtn() {
        hiliteDiagramBtn = new JRadioButton("Highlight pathway");
        PlugInObjectManager.getManager().registerRadioButton(BUTTON_GROUP_NAME,
                                                             hiliteDiagramBtn);
        // This should be selected as default
        hiliteDiagramBtn.setSelected(true);
        // The above statement should be called first so that itemStateChangedEvent
        // is not fired to avoid null exception.
        hiliteDiagramBtn.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    reHilitePathway();
            }
        });
    }
    
    protected abstract void reHilitePathway();
    
    private void synchronizeSelection() {
        selectionHandler = createSelectionHandler();
        selectionHandler.setVariableTable(contentTable);
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
    }
    
    protected VariableSelectionHandler createSelectionHandler() {
        return new VariableSelectionHandler();
    }
    
    @Override
    public void close() {
        super.close();
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        PlugInObjectManager.getManager().unregisterRadioButton(BUTTON_GROUP_NAME, hiliteDiagramBtn);
    }

    @Override
    protected void doTableSelection(ListSelectionEvent e) {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }
    
    protected void hilitePathway(int column) {
        if (hiliteControlPane == null || column < 1 || !hiliteDiagramBtn.isSelected()) // The first column is entity
            return;
        hiliteControlPane.setVisible(true);
        VariableTableModel model = (VariableTableModel) contentTable.getModel();
        Map<String, Double> idToValue = model.getIdToValue(column);
        hiliteControlPane.setIdToValue(idToValue);
        // Use 0 and 1 as default
        double[] minMaxValues = hiliteControlPane.calculateMinMaxValues(idToValue.values());
        hiliteControlPane.resetMinMaxValues(minMaxValues);
    }
    
    /**
     * Number class is used as a type in the customized NetworkModuleTableModel. However, Number
     * doesn't implement Comparable. Therefore, its sorting is based on String, which is not what
     * we want. Here, all numbers are converted into Double and then perform comparsion.
     */
    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        TableRowSorter<NetworkModuleTableModel> sorter = new TableRowSorter<NetworkModuleTableModel>(model) {
            @Override
            public Comparator<?> getComparator(int column) {
                Class<?> cls = model.getColumnClass(column);
                if (cls != Number.class)
                    return super.getComparator(column);
                // Compare based on Double values. Though the actual model class is number,
                // however, the passed values to be used for comparator are String since they
                // have been converted into String from Number. See the Java doc for TableRowSorter.
                Comparator<String> comparator = (String n1, String n2) -> {
                    Double d1 = new Double(n1);
                    Double d2 = new Double(n2);
                    return d1.compareTo(d2);
                };
                return comparator;
            }
        };
        return sorter;
    }

    protected abstract class VariableTableModel extends NetworkModuleTableModel implements VariableTableModelInterface {
        
        public VariableTableModel() {
            columnHeaders = new String[] {
                    "Entity",
            };
        }
        
        @Override
        public List<Integer> getRowsForSelectedIds(List<Long> selection) {
            List<Integer> rtn = new ArrayList<>();
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                BooleanVariable var = (BooleanVariable) row[0];
                String reactomeId = var.getProperty("reactomeId");
                if (reactomeId == null)
                    continue;
                if (selection.contains(new Long(reactomeId)))
                    rtn.add(i);
            }
            return rtn;
        }
        
        public Map<String, Double> getIdToValue(int column) {
            Map<String, Double> idToValue = new HashMap<>();
            // Use the last column
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                BooleanVariable var = (BooleanVariable) row[0];
                String id = var.getProperty("reactomeId");
                if (id == null) 
                	// Try the name
                	id = var.getName();
                if (id == null)
                    continue;
                Number value = (Number) row[column];
                if (!validateValue(value))
                    continue;
                idToValue.put(id, value.doubleValue());
            }
            return idToValue;
        }
        
        protected boolean validateValue(Number number) {
            return true;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return BooleanVariable.class;
            else if (columnIndex < 3)
                return EntityType.class;
            else 
                return Number.class;
        }
    }
}