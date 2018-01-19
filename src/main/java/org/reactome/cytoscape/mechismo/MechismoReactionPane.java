package org.reactome.cytoscape.mechismo;

import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.reactome.cytoscape.bn.SimulationComparisonPane;
import org.reactome.cytoscape.bn.VariableSelectionHandler;
import org.reactome.mechismo.model.AnalysisResult;
import org.reactome.mechismo.model.Reaction;

/**
 * This customized pane is used to display reaction level mechismo output. It customized
 * SimulationComparisonPane for Boolean network modeling.
 * @author wug
 *
 */
public class MechismoReactionPane extends SimulationComparisonPane {
    public static final String TITLE = "Mechismo Reaction";
    private JComboBox<String> cancerBox;
    
    public MechismoReactionPane() {
        super(TITLE);
    }

    /**
     * Just for sub-classing
     * @param title
     */
    protected MechismoReactionPane(String title) {
        super(title);
    }
    
    @Override
    protected void modifyContentPane() {
        super.modifyContentPane();
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        addControls();
        controlToolBar.add(closeGlue);
        createHighlightViewBtn();
        controlToolBar.add(hiliteDiagramBtn);
        controlToolBar.add(closeBtn);
    }
    
    private void addControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        // Show a list of samples
        JLabel label = new JLabel("Choose a cancer type to highlight reactions based on FDRs in the table:");
        cancerBox = new JComboBox<>();
        cancerBox.setEditable(false);
        DefaultComboBoxModel<String> sampleModel = new DefaultComboBoxModel<>();
        cancerBox.setModel(sampleModel);
        panel.add(label);
        panel.add(cancerBox);

        // Link these two boxes together
        cancerBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                handleCancerBoxSelection();
        });
        controlToolBar.add(panel);
    }
    
    private void handleCancerBoxSelection() {
        String cancer = (String) cancerBox.getSelectedItem();
        MechismoReactionModel model = (MechismoReactionModel) contentTable.getModel();
        int col = model.mapCancerToColumn(cancer);
        hilitePathway(col);
    }
    
    @Override
    protected VariableSelectionHandler createSelectionHandler() {
        return new ReactionSelectionHandler();
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new MechismoReactionModel();
    }
    
    public void setReactions(List<Reaction> reactions) {
        MechismoReactionModel model = (MechismoReactionModel) contentTable.getModel();
        model.setReactions(reactions);
        summaryLabel.setText("Mechismo reaction analysis result FDR:");
        // Need to fill in cancerbox
        ItemListener[] listener = cancerBox.getItemListeners();
        Arrays.asList(listener).forEach(l -> cancerBox.removeItem(l));
        DefaultComboBoxModel<String> cancerModel = (DefaultComboBoxModel<String>)cancerBox.getModel();
        cancerModel.removeAllElements();
        for (int i = 2; i < model.getColumnCount(); i++)
            cancerModel.addElement(model.getColumnName(i));
        Arrays.asList(listener).forEach(l -> cancerBox.addItemListener(l));
        cancerBox.setSelectedIndex(0);
    }
    
    @Override
    protected void hilitePathway(int column) {
        hiliteControlPane.setForReaction(true);
        super.hilitePathway(column);
        // Set it back
        hiliteControlPane.setForReaction(false);
    }

    protected class MechismoReactionModel extends VariableTableModel {
        
        public MechismoReactionModel() {
        }
        
        public void setReactions(List<Reaction> reactions) {
            // Grep all cancer types
            // Using jackson creates many copies of same CancerType. Therefore
            // use String instead.
            List<String> cancerTypes = grepCancerTypes(reactions);
            setUpColumnNames(cancerTypes, true);
            addValues(reactions, cancerTypes);
            fireTableStructureChanged();
        }
        
        public int mapCancerToColumn(String cancer) {
            for (int i = 0; i < columnHeaders.length; i++) {
                if (columnHeaders[i].equals(cancer))
                    return i;
            }
            return -1;
        }
        
        @Override
        public List<Integer> getRowsForSelectedIds(List<Long> selection) {
            List<Integer> rtn = new ArrayList<>();
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                Long id = (Long) row[0];
                if (selection.contains(id))
                    rtn.add(i);
            }
            return rtn;
        }
        
        @Override
        public Map<String, Double> getIdToValue(int column) {
            Map<String, Double> idToValue = new HashMap<>();
            // Use the last column
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                String id = row[0] + "";
                Double value = (Double) row[column];
                if (value != null)
                    idToValue.put(id, value);
            }
            return idToValue;
        }

        private void addValues(List<Reaction> reactions, List<String> cancerTypes) {
            reactions.sort((rxt1, rxt2) -> rxt1.getId().compareTo(rxt2.getId()));
            tableData.clear();
            reactions.forEach(rxt -> {
                Object[] row = new Object[columnHeaders.length];
                row[0] = rxt.getId();
                row[1] = rxt.getName();
                Map<String, Double> cancerToFDR = getFDRs(rxt.getAnalysisResults());
                for (int i = 0; i < cancerTypes.size(); i++) {
                    Double fdr = cancerToFDR.get(cancerTypes.get(i));
                    row[i + 2] = fdr;
                }
                tableData.add(row);
            });
        }
        
        protected Map<String, Double> getFDRs(Set<AnalysisResult> results) {
            Map<String, Double> cancerToFDR = new HashMap<>();
            if (results == null)
                return cancerToFDR;
            results.forEach(result -> cancerToFDR.put(result.getCancerType().getAbbreviation(), result.getFdr()));
            return cancerToFDR;
        }
        
        protected void setUpColumnNames(List<String> cancerTypes,
                                        boolean needId) {
            int reserved = 1;
            if (needId)
                reserved = 2;
            columnHeaders = new String[reserved + cancerTypes.size()];
            int start = 0;
            if (needId) 
                columnHeaders[start ++] = "ID";
            columnHeaders[start ++] = "Name";
            for (int i = 0; i < cancerTypes.size(); i++)
                columnHeaders[start ++] = cancerTypes.get(i);
        }
        
        private List<String> grepCancerTypes(List<Reaction> reactions) {
            Set<String> cancerTypes = new HashSet<>();
            reactions.forEach(reaction -> {
                if (reaction.getAnalysisResults() == null)
                    return;
                reaction.getAnalysisResults().forEach(result -> cancerTypes.add(result.getCancerType().getAbbreviation()));
            });
            return resortPanCancer(cancerTypes);
        }

        protected List<String> resortPanCancer(Set<String> cancerTypes) {
            List<String> list = new ArrayList<>(cancerTypes);
            Collections.sort(list);
            // Check if pancancer is there
            String pancan = null;
            for (String type : list) {
                if (type.equals("PANCAN")) {
                    pancan = type;
                    break;
                }
            }
            if (pancan != null) {
                list.remove(pancan);
                list.add(0, pancan);
            }
            return list;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Long.class;
            if (columnIndex == 1)
                return String.class;
            return Double.class;
        }
        
    }

}
