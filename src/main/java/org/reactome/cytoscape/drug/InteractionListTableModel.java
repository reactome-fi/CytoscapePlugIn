/*
 * Created on May 9, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;

public class InteractionListTableModel extends AbstractTableModel {
    protected List<String> colNames;
    private Map<String, Interaction> idToInteraction;
    protected List<Object[]> data;
    
    public InteractionListTableModel() {
//        String[] labels = new String[] {
//                "ID",
//                "Drug",
//                "Target",
//                "KD (nM)",
//                "IC50 (nM)",
//                "Ki (nM)",
//                "EC50 (nM)"
//        };
        colNames = new ArrayList<>();
    }
    
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
        resetColumns(interactions);
        initData(interactions);
    }
    
    protected void resetColumns(List<Interaction> interactions) {
        List<String> types = DrugTargetInteractionManager.getAssayTypes(interactions);
        colNames.add("ID");
        colNames.add("Drug");
        colNames.add("Target");
        colNames.add("InteractionType");
        colNames.addAll(types);
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
            Object[] row = new Object[colNames.size()];
            initRow(interaction, row);
            data.add(row);
        }
        fireTableStructureChanged();
    }

    protected void initRow(Interaction interaction, Object[] row) {
        row[0] = interaction.getId();
        row[1] = interaction.getIntDrug().getDrugName();
        row[2] = interaction.getIntTarget().getTargetName();
        row[3] = interaction.getInteractionType();
        Map<String, Double> typeToValue1 = interaction.getMinValues();
        Map<String, Double> typeToValue = typeToValue1;
        for (int i = 4; i < colNames.size(); i++) {
            row[i] = typeToValue.get(colNames.get(i));
        }
//        row[3] = typeToValue.get("KD");
//        row[4] = typeToValue.get("IC50");
//        row[5] = typeToValue.get("Ki");
//        row[6] = typeToValue.get("EC50");
    }
    
    @Override
    public int getRowCount() {
        if (data == null)
            return 0;
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return colNames.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (data == null || rowIndex >= data.size())
            return null;
        return data.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        String name = colNames.get(column);
        if (column >= 4)
            name += " (nM)";
        return name;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex < 4)
            return String.class;
        else
            return Double.class;
    }
    
}