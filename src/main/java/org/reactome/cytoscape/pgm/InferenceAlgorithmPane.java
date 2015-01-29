/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceType;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;

/**
 * A customized JPanel for setting up parameters for inference algorithms.
 * @author gwu
 *
 */
public class InferenceAlgorithmPane extends JPanel {
    private JPanel lbpPane;
    private JPanel gibbsPane;
    private JComboBox<Inferencer> algBox;
    // For what algorithm should be used
    private JRadioButton defaultAlgBtn;
    private JRadioButton selectedAlgBtn;
    
    /**
     * Default constructor.
     */
    public InferenceAlgorithmPane() {
        init();
    }
    
    /**
     * Get the selected algorithm. The client to this method should check if isOkClicked() returns
     * true. If isOkClicked() returns false, null will be returned to avoid an un-validated 
     * PGMInferenceAlgorithm object.
     * @return
     */
    public List<Inferencer> getSelectedAlgorithms() {
        List<Inferencer> rtn = new ArrayList<Inferencer>();
        if (selectedAlgBtn.isSelected()) {
            Inferencer selected = (Inferencer) algBox.getSelectedItem();
            rtn.add(selected);
        }
        else {
            for (int i = 0; i < algBox.getItemCount(); i++) {
                rtn.add(algBox.getItemAt(i));
            }
        }
        return rtn;
    }
    
    private void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // We have to call this method first to initialize the following object.
        // Otherwise, nothing is returned from the following two calls.
        //TODO: This is weird and should be changed in the future.
        FactorGraphRegistry.getRegistry(); 
        PathwayPGMConfiguration config = PathwayPGMConfiguration.getConfig();
        lbpPane = createAlgorithmPane("Loopy Belief Propagation (LBP)",
                                             config.getLBP());
        add(lbpPane);
        gibbsPane = createAlgorithmPane("Gibbs Sampling (Gibbs)",
                                               config.getGibbsSampling());
        add(gibbsPane);
        add(createAlgorithmSelectionPane());
    }
    
    private JPanel createAlgorithmSelectionPane() {
        JPanel selectionPane = new JPanel();
        selectionPane.setBorder(BorderFactory.createEtchedBorder());
        selectionPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        defaultAlgBtn = new JRadioButton("Use LBP as default. If LBP cannot converge, switch to Gibbs automatically.");
        selectionPane.add(defaultAlgBtn, constraints);
        // Set up the selected algorithm
        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        selectedAlgBtn = new JRadioButton("Use the selected algorithm:");
        selectionPane.add(selectedAlgBtn, constraints);
        setupAlgBox();
        constraints.gridx = 1;
        selectionPane.add(algBox, constraints);
        // Make sure only one button can be selected.
        ButtonGroup group = new ButtonGroup();
        group.add(defaultAlgBtn);
        group.add(selectedAlgBtn);
        defaultAlgBtn.setSelected(true); // This should be the default choice
        return selectionPane;
    }
    
    private JPanel createAlgorithmPane(String title,
                                       Inferencer algorithm) {
        JPanel algPane = new JPanel();
        Border titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                              title,
                                                              TitledBorder.LEFT,
                                                              TitledBorder.CENTER);
        algPane.setBorder(titleBorder);
        
        algPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = -1;
        Map<String, Object> keyToValue = getInferencerProps(algorithm);
        List<String> keys = new ArrayList<String>(keyToValue.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            PropertyItemPane itemPane = new PropertyItemPane();
            itemPane.setPropertyItem(key,
                                     keyToValue.get(key));
            constraints.gridy ++;
            algPane.add(itemPane, constraints);
        }
        
        return algPane;
    }
    
    private void setupAlgBox() {
        algBox = new JComboBox<Inferencer>();
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                // Just want to display a simple name for inferrers
                return super.getListCellRendererComponent(list, 
                                                          value.getClass().getSimpleName(),
                                                          index,
                                                          isSelected,
                                                          cellHasFocus);
            }
        };
        algBox.setRenderer(renderer);
        algBox.setEditable(false);
        // There are only two algorithms are supported
        PathwayPGMConfiguration config = PathwayPGMConfiguration.getConfig();
        algBox.addItem(config.getLBP());
        algBox.addItem(config.getGibbsSampling());
        // Choose the first one as the default
        algBox.setSelectedIndex(0); // The first should be LBP
    }
    
    private Map<String, Object> getInferencerProps(Inferencer alg) {
        Map<String, Object> keyToValue = new HashMap<String, Object>();
        if (alg instanceof LoopyBeliefPropagation) {
            LoopyBeliefPropagation lbp = (LoopyBeliefPropagation) alg;
            keyToValue.put("maxIteration", lbp.getMaxIteration());
            keyToValue.put("tolerance", lbp.getTolerance());
            keyToValue.put("useLogSpace", lbp.getUseLogSpace());
            keyToValue.put("dumping", lbp.getDumping());
            keyToValue.put("inferenceType", lbp.getInferenceType());
        }
        else if (alg instanceof GibbsSampling) {
            GibbsSampling gibbs = (GibbsSampling) alg;
            keyToValue.put("maxIteration", gibbs.getMaxIteration());
            keyToValue.put("burnin", gibbs.getBurnin());
            keyToValue.put("restart", gibbs.getRestart());
        }
        return keyToValue;
    }
    
    public boolean commitValues() {
        List<JPanel> propPanes = new ArrayList<JPanel>();
        propPanes.add(lbpPane);
        propPanes.add(gibbsPane);
        for (int j = 0; j < propPanes.size(); j++) {
            JPanel propertyPane = propPanes.get(j);
            Map<String, Object> keyToValue = new HashMap<String, Object>();
            for (int i = 0; i < propertyPane.getComponentCount(); i++) {
                Component comp = propertyPane.getComponent(i);
                if (comp instanceof PropertyItemPane) {
                    PropertyItemPane propPane = (PropertyItemPane) comp;
                    if (!propPane.assignPropertyValue()) 
                        return false;
                    keyToValue.put(propPane.key,
                                   propPane.value);
                }
            }
            Inferencer alg = algBox.getItemAt(j);
            if (!commitValues(keyToValue, alg))
                return false;
        }
        return true;
    }
    
    private boolean commitValues(Map<String, Object> keyToValue,
                                 Inferencer alg) {
        for (String key : keyToValue.keySet()) {
            Object value = keyToValue.get(key);
            if(!setProperty(key, value, alg))
                return false;
        }
        return true;
    }
    
    private boolean setProperty(String key,
                             Object value,
                             Object target) {
        String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
        try {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName)) {
                    method.invoke(target, value);
                    break;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Cannot set property for " + target.getClass().getSimpleName() + ": " + key + " with value " + value,
                                          "Error in Configuring Algorithm", 
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    public void reset() {
        // The first should be LBP
        Inferencer lbp = algBox.getItemAt(0);
        reset(lbpPane, lbp);
        // The second should be Gibbs
        Inferencer gibbs = algBox.getItemAt(1);
        reset(gibbsPane, gibbs);
    }
    
    private void reset(JPanel propPane, Inferencer alg) {
        Map<String, Object> propToValue = getInferencerProps(alg);
        for (int i = 0; i < propPane.getComponentCount(); i++) {
            Component comp = propPane.getComponent(i);
            if (comp instanceof PropertyItemPane) {
                PropertyItemPane propItemPane = (PropertyItemPane) comp;
                Object value = propToValue.get(propItemPane.key);
                propItemPane.resetValue(value);
            }
        }
    }
    
    /**
     * A customized JPanel to display a simple PropertyItem.
     * @author gwu
     *
     */
    private class PropertyItemPane extends JPanel {
        // Property to be displayed
        private String key;
        private Object value;
        // One of the following should be used.
        // But not both.
        private JTextField valueTF;
        private JComboBox<Object> valueBox;
        
        public PropertyItemPane() {
        }
        
        public void setPropertyItem(String key,
                                    Object value) {
            this.key = key;
            this.value = value;
            setUpGUIs();
        }
        
        private void setUpGUIs() {
            JLabel nameLabel = new JLabel(key);
            add(nameLabel);
            if (value.getClass() == Boolean.class) {
                // A special case
                valueBox = new JComboBox<Object>();
                valueBox.addItem(Boolean.TRUE);
                valueBox.addItem(Boolean.FALSE);
                valueBox.setSelectedItem(value);
                add(valueBox);
            }
            else if (value instanceof InferenceType) {
                valueBox = new JComboBox<Object>(InferenceType.values());
                valueBox.setSelectedItem(value);
                add(valueBox);
            }
            else {
                valueTF = new JTextField(value + "");
                valueTF.setColumns(4);
                add(valueTF);
            }
        }
        
        public void resetValue(Object value) {
            if (value.getClass() == Boolean.class || value instanceof InferenceType) 
                valueBox.setSelectedItem(value);
            else
                valueTF.setText(value + "");
        }
        
        public boolean assignPropertyValue() {
            if (!validValue())
                return false;
            if (valueBox != null)
                value = valueBox.getSelectedItem();
            else {
                String text = valueTF.getText().trim();
                if (value instanceof Integer)
                    value = new Integer(text);
                else if (value instanceof Double)
                    value = new Double(text);
                else
                    value = text; // General case
            }
            return true;
        }
        
        /**
         * Valid the value. 
         * @return
         */
        private boolean validValue() {
            // Only free text should be valid
            if (valueTF == null)
                return true;
            String text = valueTF.getText().trim();
            if (text.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "Value for " + key + " should not be empty.",
                                              "Empty Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (value.getClass() == Integer.class) {
                try {
                    Integer value = new Integer(text);
                }
                catch(NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Value for " + key + " should be an integer.",
                                                  "Integer Required",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            else if (value.getClass() == Double.class) {
                try {
                    Double value = new Double(text);
                }
                catch(NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Value for " + key + " should ne a number.",
                                                  "Number equired",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            return true;
        }
    }
    
}
