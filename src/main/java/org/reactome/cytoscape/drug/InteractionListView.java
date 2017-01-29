/*
 * Created on Jan 23, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.ExpEvidence;
import edu.ohsu.bcb.druggability.Interaction;

/**
 * A customized JDialog showing a list of drug/target interactions.
 * @author gwu
 *
 */
public class InteractionListView extends JDialog {
    private JTable interactionTable;
    private JButton viewDetailsBtn;
    private JButton overlayBtn;
    private TableListInteractionFilter interactionFilter;
    
    /**
     * Default constructor.
     */
    public InteractionListView() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    public void setInteractions(List<Interaction> interactions) {
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        model.setInteractions(interactions);
    }
    
    private void init() {
        setTitle("Drug Targets View");
        
        interactionTable = new JTable();
        InteractionListTableModel model = new InteractionListTableModel();
        interactionTable.setModel(model);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(model);
        interactionTable.setRowSorter(rowSorter);
        interactionFilter = new TableListInteractionFilter();
        interactionFilter.setTable(interactionTable);
        RowFilter<TableModel, Object> rowFilter = model.createFilter(interactionFilter);
        rowSorter.setRowFilter(rowFilter);
        getContentPane().add(new JScrollPane(interactionTable), BorderLayout.CENTER);
        
        JPanel controlPane = createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        interactionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    viewDetailsBtn.setEnabled(interactionTable.getSelectedRowCount() == 1);
                    overlayBtn.setEnabled(interactionTable.getSelectedRowCount() > 0);
                }
            }
        });
        
        setSize(625, 250);
        setLocationRelativeTo(getOwner());
    }
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        
        viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                viewDetails();
            }
        });
        controlPane.add(viewDetailsBtn);
        viewDetailsBtn.setEnabled(false);
        
        JButton filterBtn = new JButton("Filter Targets");
        filterBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                filterInteractions();
            }
        });
        controlPane.add(filterBtn);
        
        overlayBtn = new JButton("Overlay Targets to Pathways");
        overlayBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                overlayToPathways();
            }
        });
        overlayBtn.setEnabled(false);
        controlPane.add(overlayBtn);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.add(closeBtn);
        
        return controlPane;
    }
    
    private void filterInteractions() {
        if (interactionFilter == null) {
            interactionFilter = new TableListInteractionFilter();
            interactionFilter.setTable(interactionTable);
        }
        interactionFilter.showDialog(this);
    }
    
    private void overlayToPathways() {
        // Get the list of targets
        Set<String> targets = new HashSet<>();
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        for (int viewRow : interactionTable.getSelectedRows()) {
            int modelRow = interactionTable.convertRowIndexToModel(viewRow);
            String gene = (String) model.getValueAt(modelRow, 2); // The second column
            targets.add(gene);
        }
        dispose();
        DrugListManager.getManager().overlayTargetsToPathways(targets);
    }
    
    private void viewDetails() {
        int viewIndex = interactionTable.getSelectedRow();
        int modelIndex = interactionTable.convertRowIndexToModel(viewIndex);
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        String id = (String) model.getValueAt(modelIndex, 0);
        Interaction interaction = model.getInteraction(id);
        if (interaction == null)
            return;
        InteractionView view = new InteractionView(this);
        view.setInteraction(interaction);
        view.setModal(true);
        view.setVisible(true);
        view.toFront(); // Needed in Java 1.8.0_121 for some reason when the owner is a dialog.
    }
    
    private class TableListInteractionFilter extends InteractionFilter {
        private JTable table;
        
        public TableListInteractionFilter() {
        }
        
//        @Override
//        protected void init() {
//            // Choose all
//            dataSources = new ArrayList<>();
//            for (DataSource source : DataSource.values())
//                dataSources.add(source);
//            affinityFilters = new ArrayList<>();
//            for (AssayType type : AssayType.values()) {
//                AffinityFilter filter = new AffinityFilter();
//                filter.setAssayType(type);
//                filter.setRelation(AffinityRelation.NOGREATER);
//                affinityFilters.add(filter);
//            }
//        }
        
        public void setTable(JTable table) {
            this.table = table;
        }

        @Override
        public void applyFilter() {
            if (table == null)
                return;
            InteractionListTableModel model = (InteractionListTableModel) table.getModel();
            RowFilter<TableModel, Object> rowFilter = model.createFilter(this);
            TableRowSorter<InteractionListTableModel> rowSorter = (TableRowSorter<InteractionListTableModel>) table.getRowSorter();
            rowSorter.setRowFilter(rowFilter);
        }
        
    }
    
    private class InteractionListTableModel extends AbstractTableModel {
        private String[] colNames = new String[] {
                "ID",
                "Drug",
                "Target",
                "KD (nM)",
                "IC50 (nM)",
                "Ki (nM)",
                "EC50 (nM)"
        };
        private Map<String, Interaction> idToInteraction;
        private List<Object[]> data;
        
        public RowFilter<TableModel, Object> createFilter(final InteractionFilter filter) {
            RowFilter<TableModel, Object> rowFilter = new RowFilter<TableModel, Object>() {
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    // Entry should be a row
                    String id = entry.getStringValue(0);
                    Interaction interaction = idToInteraction.get(id);
                    return filter.filter(interaction);
                }
            };
            return rowFilter;
        }
        
        public void setInteractions(List<Interaction> interactions) {
            idToInteraction = new HashMap<>();
            for (Interaction interaction : interactions)
                idToInteraction.put(interaction.getId(), interaction);
            initData(interactions);
        }
        
        public Interaction getInteraction(String id) {
            return idToInteraction.get(id);
        }
        
        private void initData(List<Interaction> interactions) {
            if (data == null)
                data = new ArrayList<>();
            else
                data.clear();
            for (Interaction interaction : interactions) {
                Object[] row = new Object[colNames.length];
                row[0] = interaction.getId();
                row[1] = interaction.getIntDrug().getDrugName();
                row[2] = interaction.getIntTarget().getTargetName();
                Map<String, Double> typeToValue = getMinValues(interaction);
                row[3] = typeToValue.get("KD");
                row[4] = typeToValue.get("IC50");
                row[5] = typeToValue.get("Ki");
                row[6] = typeToValue.get("EC50");
                data.add(row);
            }
            fireTableDataChanged();
        }
        
        private Map<String, Double> getMinValues(Interaction interaction) {
            Map<String, Double> typeToValue = new HashMap<>();
            if (interaction.getExpEvidenceSet() != null) {
                for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                    String type = evidence.getAssayType();
                    if (type == null)
                        continue;
                    if (type.equals("KI"))
                        type = "Ki";
                    double value = DrugTargetInteractionManager.getManager().getExpEvidenceValue(evidence).doubleValue();
                    if (!typeToValue.containsKey(type) || value < typeToValue.get(type))
                        typeToValue.put(type, value);
                }
            }
            return typeToValue;
        }

        @Override
        public int getRowCount() {
            if (data == null)
                return 0;
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null || rowIndex >= data.size())
                return null;
            return data.get(rowIndex)[columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex < 3)
                return String.class;
            else
                return Double.class;
        }
        
    }
    
}
