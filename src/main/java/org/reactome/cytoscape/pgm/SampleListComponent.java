/*
 * Created on Dec 2, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;

/**
 * @author gwu
 *
 */
public class SampleListComponent extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Sample List";
    // GUIs
    private JComboBox<String> sampleBox;
    private JPanel typePane;
    private JLabel typeBox;
    // Data to be displayed
    private Map<String, String> sampleToType;
    
    public SampleListComponent() {
        init();
    }
    
    private void init() {
        initGUIs();
        PlugInUtilities.registerCytoPanelComponent(this);
    }

    private void initGUIs() {
        // Set up GUIs
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        initNorthPane();
        
        // Use JTabbedPane for showing observations and inference results
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addTab("Observation", new JScrollPane(new JTable()));
        tabbedPane.addTab("Inference", new JScrollPane(new JTable()));
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initNorthPane() {
        // There are two panels in the north: one for listing samples
        // another for showing sample type if any
        JPanel northPane = new JPanel();
        northPane.setLayout(new BoxLayout(northPane, BoxLayout.Y_AXIS));
        
        // Show a list of samples
        JLabel sampleLabel = new JLabel("Choose sample:");
        sampleBox = new JComboBox<>();
        sampleBox.setEditable(false);
        DefaultComboBoxModel<String> sampleModel = new DefaultComboBoxModel<>();
        sampleBox.setModel(sampleModel);
        JPanel samplePane = new JPanel();
        samplePane.setBorder(BorderFactory.createEtchedBorder());
        samplePane.add(sampleLabel);
        samplePane.add(sampleBox);
        northPane.add(samplePane);
        
        // Show type information
        JLabel typeLabel = new JLabel("Type:");
        typeBox = new JLabel();
        typeBox.setBackground(Color.WHITE);
        typePane = new JPanel();
        typePane.setBorder(BorderFactory.createEtchedBorder());
        typePane.add(typeLabel);
        typePane.add(typeBox);
        northPane.add(typePane);
        
        // Link these two boxes together
        sampleBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                handleSampleSelection();
            }
        });
        
        add(northPane, BorderLayout.NORTH);
    }

    private void handleSampleSelection() {
        if (sampleToType == null || sampleToType.size() == 0)
            return;
        String type = sampleToType.get(sampleBox.getSelectedItem());
        if (type == null)
            typeBox.setText("");
        else
            typeBox.setText(type);
    }
    
    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
    
    /**
     * Set the inference results to be displayed in this component.
     * @param results
     */
    public void setInferenceResults(FactorGraphInferenceResults results) {
        // Initialize the sample box
        DefaultComboBoxModel<String> sampleModel = (DefaultComboBoxModel<String>) sampleBox.getModel();
        sampleModel.removeAllElements();
        List<String> sampleNames = getSampleNames(results);
        for (String sampleName : sampleNames)
            sampleModel.addElement(sampleName);
        sampleBox.setSelectedIndex(0);
        sampleToType = results.getSampleToType();
        if (sampleToType == null || sampleToType.size() == 0)
            typePane.setVisible(false);
        else {
            typePane.setVisible(true);
            // Somehow we have to set the type manually
            handleSampleSelection();
        }
    }
    
    private List<String> getSampleNames(FactorGraphInferenceResults results) {
        List<String> sampleNames = new ArrayList<>();
        for (Observation<Number> obs : results.getObservations())
            sampleNames.add(obs.getName());
        Collections.sort(sampleNames);
        return sampleNames;
    }
    
}
