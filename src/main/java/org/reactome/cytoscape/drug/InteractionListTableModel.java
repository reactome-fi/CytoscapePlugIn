/*
 * Created on May 9, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import edu.ohsu.bcb.druggability.ExpEvidence;
import edu.ohsu.bcb.druggability.Interaction;

public class InteractionListTableModel extends AbstractTableModel {
    protected String[] colNames = new String[] {
            "ID",
            "Drug",
            "Target",
            "KD (nM)",
            "IC50 (nM)",
            "Ki (nM)",
            "EC50 (nM)"
    };
    private Map<String, Interaction> idToInteraction;
    protected List<Object[]> data;
    
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
            initRow(interaction, row);
            data.add(row);
        }
        fireTableDataChanged();
    }

    protected void initRow(Interaction interaction, Object[] row) {
        row[0] = interaction.getId();
        row[1] = interaction.getIntDrug().getDrugName();
        row[2] = interaction.getIntTarget().getTargetName();
        Map<String, Double> typeToValue = getMinValues(interaction);
        row[3] = typeToValue.get("KD");
        row[4] = typeToValue.get("IC50");
        row[5] = typeToValue.get("Ki");
        row[6] = typeToValue.get("EC50");
    }
    
    private Map<String, Double> getMinValues(Interaction interaction) {
        Map<String, Double> typeToValue = new HashMap<>();
        if (interaction.getExpEvidenceSet() != null) {
            for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                if (DrugTargetInteractionManager.getManager().shouldFilterOut(evidence))
                    continue;
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