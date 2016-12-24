/*
 * Created on Dec 23, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JDialog for filtering drug/target interactions.
 * @author gwu
 *
 */
public class InteractionFilter {
    
    private List<DataSource> dataSources; // Databases and pubmed
    private List<AffinityFilter> affinityFilters;
    
    public InteractionFilter() {
        init();
    }
    
    private void init() {
        // Choose all
        dataSources = new ArrayList<>();
        for (DataSource source : DataSource.values())
            dataSources.add(source);
        affinityFilters = new ArrayList<>();
        for (AssayType type : AssayType.values()) {
            AffinityFilter filter = new AffinityFilter();
            filter.setAssayType(type);
            affinityFilters.add(filter);
        }
    }
    
    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }
    
    public void addDataSource(DataSource source) {
        if (this.dataSources == null)
            dataSources = new ArrayList<>();
        if (dataSources.contains(source))
            return;
        dataSources.add(source);
    }
    
    public void removeDataSource(DataSource source) {
        if (dataSources == null)
            return;
        dataSources.remove(source);
    }

    public List<AffinityFilter> getAffinityFilters() {
        return affinityFilters;
    }

    public void setAffinityFilters(List<AffinityFilter> affinityFilters) {
        this.affinityFilters = affinityFilters;
    }
    
    public void removeAffinityFilter(AssayType type) {
        if (affinityFilters == null)
            return;
        AffinityFilter existed = getExistedAffinityFilter(type);
        if (existed != null)
            affinityFilters.remove(existed);
    }
    
    public void addAffinityFilter(AffinityFilter affinityFilter) {
        if (affinityFilters == null)
            affinityFilters = new ArrayList<>();
        AffinityFilter existed = getExistedAffinityFilter(affinityFilter.getAssayType());
        if (existed == null)
            affinityFilters.add(affinityFilter);
        else {
            // Copy values
            existed.setRelation(affinityFilter.getRelation());
            existed.setValue(affinityFilter.getValue());
        }
    }

    private AffinityFilter getExistedAffinityFilter(AssayType type) {
        // Find if there is an AffinityFilter exists with the same type
        AffinityFilter existed = null;
        for (AffinityFilter old : affinityFilters) {
            if (old.getAssayType() == type) {
                existed = old;
                break;
            }
        }
        return existed;
    }

    public void showDialog(ActionListener applyAction) {
        InteractionFilterDialog dialog = new InteractionFilterDialog();
        dialog.okBtn.addActionListener(applyAction);
        dialog.setModal(false); // So that we can see the action of filtering
        dialog.setVisible(true);
    }
    
    public static void main(String[] args) {
        InteractionFilter filter = new InteractionFilter();
        filter.removeDataSource(DataSource.database);
        filter.removeAffinityFilter(AssayType.Ki);
        final InteractionFilterDialog dialog = new InteractionFilterDialog();
        dialog.setFilter(filter);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
    
    private static class InteractionFilterDialog extends JDialog {
        // The data model
        private InteractionFilter filter;
        private JButton okBtn;
        // Checkboxes for data sources
        private Map<DataSource, JCheckBox> sourceToBox;
        private Map<AssayType, JCheckBox> typeToBox;
        private Map<AssayType, JComboBox<AffinityRelation>> typeToRelationBox;
        private Map<AssayType, JTextField> typeToValueBox;
        
        public InteractionFilterDialog() {
//            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        public void setFilter(InteractionFilter filter) {
            this.filter = filter;
            // Need to update GUIs
            // Update source boxes
            updateSourceBoxes();
            updateAffinityGUIs();
        }
        
        private void updateSourceBoxes() {
            List<DataSource> sources = filter.getDataSources();
            for (DataSource source : sourceToBox.keySet()) {
                JCheckBox box = sourceToBox.get(source);
                if (sources != null && sources.contains(source))
                    box.setSelected(true);
                else
                    box.setSelected(false);
            }
        }
        
        private void updateAffinityGUIs() {
            for (AssayType type : typeToBox.keySet()) {
                JCheckBox box = typeToBox.get(type);
                box.setSelected(false); // De-selected them first
            }
            for (AffinityFilter aFilter : filter.getAffinityFilters()) {
                AssayType type = aFilter.getAssayType();
                JCheckBox box = typeToBox.get(type);
                box.setSelected(true);
                JComboBox<AffinityRelation> relBox = typeToRelationBox.get(type);
                if (aFilter.getRelation() != null)
                    relBox.setSelectedItem(aFilter.getRelation());
                JTextField valueBox = typeToValueBox.get(type);
                if (aFilter.getValue() == null)
                    valueBox.setText("");
                else
                    valueBox.setText(aFilter.getValue().toString());
            }
        }

        private void init() {
            setTitle("Drug/Target Interaction Filter");
            setSize(345, 370);
            setLocationRelativeTo(getOwner());
            
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            JPanel sourcePane = createSourcePanel();
            contentPane.add(sourcePane);
            contentPane.add(Box.createVerticalStrut(5));
            JPanel affinityPane = createAffinityPanel();
            contentPane.add(affinityPane);
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            controlPane.getOKBtn().setText("Apply");
            controlPane.getCancelBtn().setText("Close");
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            this.okBtn = controlPane.getOKBtn();
        }
        
        private JPanel createSourcePanel() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose support sources:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            pane.add(label, constraints);
            sourceToBox = new HashMap<>();
            for (DataSource source : DataSource.values()) {
                constraints.gridy ++;
                JCheckBox sourceBox = new JCheckBox(source.toString());
                sourceBox.setSelected(true);
                pane.add(sourceBox,
                         constraints);
                sourceToBox.put(source, sourceBox);
            }
            return pane;
        }
        
        private JPanel createAffinityPanel() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose affinities (empty means all):");
            constraints.gridwidth = 3;
            constraints.gridx = 0;
            constraints.gridy = 0;
            pane.add(label, constraints);
            constraints.gridwidth = 1;
            typeToBox = new HashMap<>();
            typeToRelationBox = new HashMap<>();
            typeToValueBox = new HashMap<>();
            for (AssayType type : AssayType.values()) {
                constraints.gridy ++;
                createAffinityRelationGUI(type, pane, constraints);
            }
            
            return pane;
        }
        
        private void createAffinityRelationGUI(AssayType type,
                                               JPanel pane,
                                               GridBagConstraints constraints) {
            JCheckBox box = new JCheckBox(type.toString());
            box.setSelected(true);
            typeToBox.put(type, box);
            constraints.gridx = 0;
            pane.add(box, constraints);
            constraints.gridx = 1;
            JComboBox<AffinityRelation> relBox = new JComboBox<>();
            typeToRelationBox.put(type, relBox);
            for (AffinityRelation rel : AffinityRelation.values()) {
                relBox.addItem(rel);
            }
            relBox.setSelectedIndex(0);
            pane.add(relBox, constraints);
            constraints.gridx = 2;
            JTextField valueBox = new JTextField();
            valueBox.setColumns(8);
            typeToValueBox.put(type, valueBox);
            pane.add(valueBox, constraints);
        }
    }
    
    static class AffinityFilter {
        private AssayType assayType;
        private AffinityRelation relation;
        private Double value;
        
        public AssayType getAssayType() {
            return assayType;
        }
        public void setAssayType(AssayType assayType) {
            this.assayType = assayType;
        }
        public AffinityRelation getRelation() {
            return relation;
        }
        public void setRelation(AffinityRelation relation) {
            this.relation = relation;
        }
        public Double getValue() {
            return value;
        }
        public void setValue(Double value) {
            this.value = value;
        }
    }
    
    static enum AssayType {
        EC50,
        IC50, 
        KD,
        Ki
    }
    
    static enum DataSource {
        database,
        pubmed
    }
    
    static enum AffinityRelation {
        NOGREATER ("<="),
        LESS ("<"), 
        EQUAL ("="),
        NOLESS (">="),
        GREATER (">");
        
        private String symbol;
        
        AffinityRelation(String symbol) {
            this.symbol = symbol;
        }
        
        @Override
        public String toString() {
            return this.symbol;
        }
    }
}
