/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.gk.graphEditor.SelectionMediator;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.bn.BooleanNetworkSamplePane.EntityType;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * @author gwu
 *
 */
public abstract class VariableCytoPaneComponent extends NetworkModulePanel {
    private final String BUTTON_GROUP_NAME = "HighlightPathway";
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
        selectionHandler = new VariableSelectionHandler();
        selectionHandler.setVariableTable(contentTable);
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
        
        contentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleTableSelection();
            }
        });
    }
    
    @Override
    public void close() {
        super.close();
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        PlugInObjectManager.getManager().unregisterRadioButton(BUTTON_GROUP_NAME, hiliteDiagramBtn);
    }

    private void handleTableSelection() {
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