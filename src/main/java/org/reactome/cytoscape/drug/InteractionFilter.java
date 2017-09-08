/*
 * Created on Dec 23, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.service.CyPathwayEditor;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;
import edu.ohsu.bcb.druggability.dataModel.Source;

/**
 * A customized JDialog for filtering drug/target interactions.
 * @author gwu
 *
 */
public class InteractionFilter {
    // Use 1000 nM as the default
    private final Double DEFAULT_MAX_VALUE = 1000d; // 100 nM
    
    private List<DataSource> dataSources; // Databases and pubmed
    private List<AffinityFilter> affinityFilters;
    
    // Check name
    private NameFilter nameFilter;
    
    // The target of this filter applies to
    private CyPathwayEditor pathwayEditor;
    // Cache the dialog so that there is only one displayed
    private JDialog dialog;
    
    public InteractionFilter() {
        init();
    }
    
    public NameFilter getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(NameFilter nameFilter) {
        this.nameFilter = nameFilter;
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
            filter.setRelation(AffinityRelation.NOGREATER);
            filter.setValue(DEFAULT_MAX_VALUE);
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
    
    public void resetAffinityFilterValues() {
        if (affinityFilters == null)
            return;
        affinityFilters.stream().forEach(filter -> filter.setValue(""));
    }
    
    /**
     * The actual method to perform filter.
     * @param interaction
     * @return true if the interaction should NOT be filtered out.
     */
    public boolean filter(Interaction interaction) {
        // Check if pubmed is selected
        if (getDataSources() != null && getDataSources().size() > 0) {
            // There is only one source, pubmed
            if (!hasPubMedSource(interaction))
                return false;
        }
        // Check name
        if (nameFilter != null) {
            String drugName = interaction.getIntDrug().getDrugName();
            if (!nameFilter.filter(drugName))
                return false;
        }
        // Get relation ship. We use "OR" relationship here
        boolean rtn = true;
        if (getAffinityFilters() != null && getAffinityFilters().size() > 0) {
            rtn = checkAffinityFilters(interaction);
        }
        return rtn;
    }

    private boolean checkAffinityFilters(Interaction interaction) {
        boolean rtn;
        rtn = false;
        for (AffinityFilter filter : getAffinityFilters()) {
            rtn |= checkAffinityFilter(filter, interaction);
        }
        // A special case when there is no affinity data available
        if (!rtn && interaction.getExpEvidenceSet() == null) {
            rtn = true; // In this case, default will be true
            for (AffinityFilter filter : getAffinityFilters()) {
                if (filter.getValue() != null) {
                    rtn = false;
                    break;
                }
            }
        }
        return rtn;
    }
    
    public void applyFilter() {
        if (pathwayEditor == null)
            return; // Don't do anything
        DrugTargetInteractionManager.getManager().applyFilter(pathwayEditor);
    }
    
    private boolean checkAffinityFilter(AffinityFilter filter,
                                        Interaction interaction) {
        boolean rtn = false;
        if (interaction.getExpEvidenceSet() != null) {
            DrugTargetInteractionManager manager = DrugTargetInteractionManager.getManager();
            // Since there are multiple values for the same assay type, we should not return
            // when the type is found. All values should be checked.
            for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                if (manager.shouldFilterOut(evidence))
                    continue;
                String assayType = evidence.getAssayType().toUpperCase();
                if (!assayType.equals(filter.getAssayType().toString().toUpperCase()))
                    continue;
                Double refValue = filter.getValue();
                if (refValue == null)
                    return true; // Use all values
                Number value = manager.getExpEvidenceValue(evidence);
                rtn = AffinityRelation.compare(value.doubleValue(),
                                                refValue,
                                                filter.getRelation());
                if (rtn)
                    return true; // Otherwise, continue search
            }
        }
        return false;
    }
    
    private boolean hasPubMedSource(Interaction interaction) {
        if (interaction.getInteractionSourceSet() != null) {
            for (Source source : interaction.getInteractionSourceSet()) {
                if (source.getSourceLiterature() != null) {
                    String pubmedId = source.getSourceLiterature().getPubMedID();
                    if (pubmedId != null && pubmedId.matches("\\d+"))
                        return true;
                }
            }
        }
        if (interaction.getExpEvidenceSet() != null) {
            for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                if (evidence.getExpSourceSet() != null) {
                    for (Source source : evidence.getExpSourceSet()) {
                        if (source.getSourceLiterature() != null) {
                            String pubmedId = source.getSourceLiterature().getPubMedID();
                            if (pubmedId != null && pubmedId.matches("\\d+"))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
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
    
    public void setPathwayEditor(CyPathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }
    
    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(Window owner) {
        if (this.dialog != null) {
            this.dialog.setVisible(true);
            this.dialog.toFront();
            return;
        }
        InteractionFilterDialog dialog = null;
        if (owner == null)
            dialog = new InteractionFilterDialog();
        else
            dialog = new InteractionFilterDialog(owner);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                InteractionFilter.this.dialog = null;
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                InteractionFilter.this.dialog = null;
            }
        });
        dialog.setFilter(this);
        dialog.okBtn.setEnabled(false); // Reset it as false
        // It seems that there is a bug in the latest version of Java (Java 1.8.0_121) regarding
        // the relationships among JDialogs: dialog here cannot be displayed correctly after
        // a detailed interaction dialog is shown when dialog has modal = false. Therefore,
        // do the following:
        dialog.setModal(dialog.getOwner() instanceof JDialog); 
//        dialog.setModal(false); // So that we can see the action of filtering
        dialog.setVisible(true);
        this.dialog = dialog;
    }
    
    public static void main(String[] args) {
        InteractionFilter filter = new InteractionFilter();
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
        // Drug name
        private JTextField drugNameTF;
        private JCheckBox wholeNameBox;
        // To enable okBtn
        private ActionListener okBtnEnabled;
        private DocumentListener tfOkBtnEnabled;
        
        public InteractionFilterDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        public InteractionFilterDialog(Window owner) {
            super(owner);
            init();
        }
        
        public void setFilter(InteractionFilter filter) {
            this.filter = filter;
            // Need to update GUIs
            // Update source boxes
            updateSourceBoxes();
            updateAffinityGUIs();
            updateNameGUIs();
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
            setSize(425, 390);
            setLocationRelativeTo(getOwner());
            
            okBtnEnabled = new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (okBtn != null)
                        okBtn.setEnabled(true);
                }
            };
            
            tfOkBtnEnabled = new DocumentListener() {
                
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
            
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            JPanel sourcePane = createSourcePanel();
            contentPane.add(sourcePane);
            JPanel affinityPane = createAffinityPanel();
            contentPane.add(affinityPane);
            JPanel namePane = createNamePanel();
            contentPane.add(namePane);
            
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
            this.okBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyFilter();
                }
            });
            this.okBtn.setEnabled(false);
        }
        
        private void applyFilter() {
            // Update filter
            for (DataSource source : sourceToBox.keySet()) {
                JCheckBox box = sourceToBox.get(source);
                if (box.isSelected())
                    filter.addDataSource(source);
                else
                    filter.removeDataSource(source);
            }
            // Update name
            if (drugNameTF.getText().trim().length() == 0) {
                filter.setNameFilter(null);
            }
            else {
                NameFilter nameFilter = new NameFilter();
                nameFilter.setNamePattern(drugNameTF.getText().trim());
                nameFilter.setMatchWholeNameOnly(wholeNameBox.isSelected());
                filter.setNameFilter(nameFilter);
            }
            // Update affinity relationships
            for (AssayType type : typeToBox.keySet()) {
                JCheckBox box = typeToBox.get(type);
                if (box.isSelected()) {
                    AffinityFilter aFilter = new AffinityFilter();
                    aFilter.setAssayType(type);
                    aFilter.setRelation((AffinityRelation)(typeToRelationBox.get(type).getSelectedItem()));
                    String value = typeToValueBox.get(type).getText().trim();
                    if (value != null)
                        aFilter.setValue(value);
                    filter.addAffinityFilter(aFilter);
                }
                else
                    filter.removeAffinityFilter(type);
            }
            filter.applyFilter();
        }
        
        private JPanel createSourcePanel() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = createConstraints();
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose support source:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            pane.add(label, constraints);
            sourceToBox = new HashMap<>();
            for (DataSource source : DataSource.values()) {
                constraints.gridy ++;
                JCheckBox sourceBox = new JCheckBox(source.toString());
                sourceBox.addActionListener(okBtnEnabled);
                sourceBox.setSelected(true);
                pane.add(sourceBox,
                         constraints);
                sourceToBox.put(source, sourceBox);
            }
            return pane;
        }
        
        private void updateNameGUIs() {
            NameFilter nameFilter = filter.getNameFilter();
            if (nameFilter == null)
                return;
            drugNameTF.setText(nameFilter.getNamePattern());
            wholeNameBox.setSelected(nameFilter.isMatchWholeNameOnly());
        }
        
        private JPanel createNamePanel() {
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEtchedBorder());
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = createConstraints();
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose drugs:");
            drugNameTF = new JTextField();
            drugNameTF.getDocument().addDocumentListener(tfOkBtnEnabled);
            drugNameTF.setColumns(20);
            wholeNameBox = new JCheckBox("Match whole name only");
            
            panel.add(label, constraints);
            constraints.gridy = 1;
            panel.add(drugNameTF, constraints);
            constraints.gridy = 2;
            panel.add(wholeNameBox, constraints);
            
            return panel;
        }

        protected GridBagConstraints createConstraints() {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            return constraints;
        }
        
        private JPanel createAffinityPanel() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = createConstraints();
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose affinities (Unit: nM. Empty means all):");
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
                createAffinityRelationGUI(type, pane, constraints, tfOkBtnEnabled);
            }
            
            return pane;
        }
        
        private void createAffinityRelationGUI(AssayType type,
                                               JPanel pane,
                                               GridBagConstraints constraints,
                                               DocumentListener docListner) {
            JCheckBox box = new JCheckBox(type.toString());
            box.addActionListener(okBtnEnabled);
            box.setSelected(true);
            typeToBox.put(type, box);
            constraints.gridx = 0;
            pane.add(box, constraints);
            constraints.gridx = 1;
            JComboBox<AffinityRelation> relBox = new JComboBox<>();
            relBox.addActionListener(okBtnEnabled);
            typeToRelationBox.put(type, relBox);
            for (AffinityRelation rel : AffinityRelation.values()) {
                relBox.addItem(rel);
            }
            relBox.setSelectedIndex(0);
            pane.add(relBox, constraints);
            constraints.gridx = 2;
            JTextField valueBox = new JTextField();
            valueBox.getDocument().addDocumentListener(docListner);
            valueBox.setColumns(8);
            typeToValueBox.put(type, valueBox);
            pane.add(valueBox, constraints);
        }
    }
    
    private static class NameFilter {
        private boolean matchWholeNameOnly;
        private String namePattern;
        
        public NameFilter() {
        }

        public boolean isMatchWholeNameOnly() {
            return matchWholeNameOnly;
        }

        public void setMatchWholeNameOnly(boolean matchWholeNameOnly) {
            this.matchWholeNameOnly = matchWholeNameOnly;
        }

        /**
         * Return true if the name IS matched.
         * @param name
         * @return
         */
        public boolean filter(String name) {
            if (matchWholeNameOnly)
                return name.equals(namePattern);
            else
                return name.contains(namePattern);
        }

        public String getNamePattern() {
            return namePattern;
        }

        public void setNamePattern(String namePattern) {
            this.namePattern = namePattern;
        }
    }
    
    private static class AffinityFilter {
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
        public void setValue(String value) {
            if (value == null || value.length() == 0)
                this.value = null;
            else {
                try {
                    Double dValue = new Double(value);
                    setValue(dValue);
                }
                catch(NumberFormatException e) {
                }
            }
        }
    }
    
    static enum AssayType {
        KD,
        IC50,
        Ki,
        EC50
    }
    
    /**
     * Since all interactions are extracted from one or more databases, there is no
     * need to filter based on database.
     * @author gwu
     *
     */
    static enum DataSource {
//        database,
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
        
        public static boolean compare(double target,
                                      double ref,
                                      AffinityRelation rel) {
            switch (rel) {
                case NOGREATER : return target - ref <= 0.0d;
                case LESS : return target - ref < 0.0d;
                case EQUAL : return Math.abs(target - ref) < 1.0e-6; 
                case NOLESS : return target - ref >= 0.0d;
                case GREATER : return target - ref > 0.0d;
            }
            return false;
        }
    }
}
