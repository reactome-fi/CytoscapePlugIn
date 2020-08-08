package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.gk.util.GKApplicationUtilities;
import org.gk.util.StringUtils;
import org.reactome.cytoscape.drug.InteractionListTableModel;
import org.reactome.cytoscape.drug.InteractionListView;
import org.reactome.pathway.booleannetwork.AffinityToModificationMap;
import org.reactome.pathway.booleannetwork.DefaultAffinityToModificationMap;
import org.reactome.pathway.booleannetwork.DrugTargetInteractionTypeMapper;
import org.reactome.pathway.booleannetwork.ModificationType;

import edu.ohsu.bcb.druggability.dataModel.Interaction;

public class DrugSelectionDialog extends InteractionListView {
    boolean isOKClicked;
    private JLabel titleLabel;

    public DrugSelectionDialog(JDialog owner) {
        super(owner);
    }

    @Override
    public void setInteractions(List<Interaction> interactions) {
        super.setInteractions(interactions);
        modifyTable();
    }

    @Override
    protected TableListInteractionFilter createInteractionFilter() {
        TableListInteractionFilter filter = super.createInteractionFilter();
        filter.resetAffinityFilterValues();
        return filter;
    }

    @Override
    protected JComponent createContentPane() {
        return super.createTablePane();
    }

    @Override
    protected JPanel createDialogControlPane() {
        JPanel controlPane = super.createTableControlPane();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        controlPane.add(closeBtn);
        return controlPane;
    }

    @Override
    protected JPanel createTableControlPane() {
        return null;
    }

    @Override
    protected void init() {
        super.init();
        setTitle("Drug Selection");
        titleLabel = GKApplicationUtilities.createTitleLabel("Choose drugs by selecting:");
        titleLabel.setToolTipText("Note: Selecting one row will select all rows for the selected drug.");
        getContentPane().add(titleLabel, BorderLayout.NORTH);
        // Since the column is dynamically generated, we need to call this later on.
        //            modifyTable();
    }

    public Map<String, Double> getInhibition() {
        return getModification(ModificationType.Inhibition);
    }

    public Map<String, Double> getActivation() {
        return getModification(ModificationType.Activation);
    }

    private Map<String, Double> getModification(ModificationType type) {
        List<String> drugs = getSelectedDrugs();
        if (drugs.size() == 0)
            return new HashMap<>();
        Map<String, Double> targetToValue = new HashMap<>();
        DrugSelectionTableModel model = (DrugSelectionTableModel) interactionTable.getModel();
        int col = model.getColumnCount();
        for (int i = 0; i < model.getRowCount(); i++) {
            String drug = (String) model.getValueAt(i, 1);
            if (!drugs.contains(drug))
                continue;
            ModificationType type1 = (ModificationType) model.getValueAt(i, col - 2);
            if (type1 != type)
                continue;
            Double value = (Double) model.getValueAt(i, col - 1);
            if (value == null)
                continue; // Don't want to have null
            String target = (String) model.getValueAt(i, 2);
            targetToValue.put(target, value);
        }
        return targetToValue;
    }

    public List<String> getSelectedDrugs() {
        List<String> rtn = new ArrayList<>();
        String title = titleLabel.getText().trim();
        int index = title.indexOf(":");
        String text = title.substring(index + 1).trim();
        if (text.length() == 0)
            return rtn;
        String[] tokens = text.split(", ");
        for (String token : tokens)
            rtn.add(token);
        return rtn;
    }

    private void modifyTable() {
        // Handle table editing
        DefaultTableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer();
        int col = interactionTable.getColumnCount();
        TableColumn modificationCol = interactionTable.getColumnModel().getColumn(col - 2);
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

        TableColumn initCol = interactionTable.getColumnModel().getColumn(col - 1);
        initCol.setCellEditor(numberEditor);
        initCol.setCellRenderer(defaultCellRenderer);

        // Sort based on the drug name
        List<SortKey> sortedKeys = new ArrayList<>();
        sortedKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        interactionTable.getRowSorter().setSortKeys(sortedKeys);
    }

    @Override
    protected InteractionListTableModel createTableModel() {
        return new DrugSelectionTableModel();
    }

    @Override
    protected void handleTableSelection() {
        super.handleTableSelection();
        // Get selected drugs
        TableModel model = interactionTable.getModel();
        Set<String> drugs = new HashSet<>();
        if (interactionTable.getSelectedRowCount() > 0) {
            for (int viewRow : interactionTable.getSelectedRows()) {
                int modelRow = interactionTable.convertRowIndexToModel(viewRow);
                String drug = (String) model.getValueAt(modelRow, 1);
                drugs.add(drug);
            }
        }
        List<String> drugList = new ArrayList<>(drugs);
        Collections.sort(drugList);
        String text = StringUtils.join(", ", drugList);
        String title = titleLabel.getText();
        int index = title.indexOf(":");
        title = title.substring(0, index + 1) + " " + text;
        titleLabel.setText(title);
    }

    @Override
    protected JButton createActionButton() {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                isOKClicked = true;
                dispose();
            }
        });
        return okButton;
    }

    class DrugSelectionTableModel extends InteractionListTableModel {
        // Used to map from affinity to modification strength
        private AffinityToModificationMap affinityToModificationMap;
        private DrugTargetInteractionTypeMapper typeManager;

        public DrugSelectionTableModel() {
            super();
            //            String[] labels = new String[] {
            //                    "ID",
            //                    "Drug",
            //                    "Target",
            //                    "KD (nM)",
            //                    "IC50 (nM)",
            //                    "Ki (nM)",
            //                    "EC50 (nM)",
            //                    "Modification",
            //                    "Strength"
            //            };
            affinityToModificationMap = new DefaultAffinityToModificationMap();
            typeManager = new DrugTargetInteractionTypeMapper();
        }

        @Override
        protected void resetColumns(List<Interaction> interactions) {
            super.resetColumns(interactions);
            // Need to add the following extra columns
            //          "Modification",
            //          "Strength"
            colNames.add("Modification");
            colNames.add("Strength");
        }

        @Override
        protected void initRow(Interaction interaction, Object[] row) {
            super.initRow(interaction, row);
            row[colNames.size() - 2] = typeManager.getModificationType(interaction.getInteractionType());
            Double minValue = getMinValue(row);
            if (minValue == null)
                row[colNames.size() - 1] = null;
            else {
                row[colNames.size() - 1] = getModificationStrenth(minValue);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object[] rowValues = data.get(rowIndex);
            rowValues[columnIndex] = aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex >= colNames.size() - 2)
                return true;
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == colNames.size() - 2)
                return ModificationType.class;
            else if (columnIndex == colNames.size() - 1)
                return Double.class;
            else
                return super.getColumnClass(columnIndex);
        }

        @Override
        public String getColumnName(int column) {
            if (column >= colNames.size() - 2)
                return colNames.get(column);
            return super.getColumnName(column);
        }

        /**
         * The following map is hard-coded and should be improved in the future.
         * @param value
         * @return
         */
        private double getModificationStrenth(double value) {
            return affinityToModificationMap.getModificationStrenth(value);
            //            if (value < 1) // Less than 1 nm
            //                return 0.99d;
            //            if (value < 10)
            //                return 0.90d;
            //            if (value < 100)
            //                return 0.70;
            //            if (value < 1000)
            //                return 0.50;
            //            if (value < 10000)
            //                return 0.30;
            //            if (value < 100000)
            //                return 0.10;
            //            return 0.0d;
        }

        private Double getMinValue(Object[] row) {
            Double rtn = null;
            for (int i = 4; i < colNames.size() - 2; i++) {
                Double value = (Double) row[i];
                if (value == null)
                    continue;
                if (rtn == null)
                    rtn = value;
                else if (rtn > value)
                    rtn = value;
            }
            return rtn;
        }
    }

}