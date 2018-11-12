/*
 * Created on Sep 10, 2015
 *
 */
package org.reactome.cytoscape.service;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceType;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;

/**
 * A customized JPanel to show properties for an PGM inference algorithm.
 * @author gwu
 *
 */
public class PGMAlgorithmPanel extends JPanel {
    private Inferencer algorithm;
    
    /**
     * Default constructor.
     */
    public PGMAlgorithmPanel(String title,
                             Inferencer algorithm) {
        if (title == null || algorithm == null)
            throw new NullPointerException("Both parameters for PGMAlgorithmPanel shoul not be null.");
        init(title, algorithm);
    }
    
    /**
     * Get the wrapped Inferencer for configuring.
     * @return
     */
    public Inferencer getAlgorithm() {
        return this.algorithm;
    }
    
    private void init(String title, 
                      Inferencer algorithm) {
        Border titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                              title,
                                                              TitledBorder.LEFT,
                                                              TitledBorder.CENTER);
        setBorder(titleBorder);
        
        setLayout(new GridBagLayout());
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
            add(itemPane, constraints);
        }
        this.algorithm = algorithm;
    }
    
    private Map<String, Object> getInferencerProps(Inferencer alg) {
        Map<String, Object> keyToValue = new HashMap<String, Object>();
        if (alg instanceof LoopyBeliefPropagation) {
            LoopyBeliefPropagation lbp = (LoopyBeliefPropagation) alg;
            keyToValue.put("maxIteration", lbp.getMaxIteration());
            keyToValue.put("tolerance", lbp.getTolerance());
            keyToValue.put("useLogSpace", lbp.getUseLogSpace());
//            keyToValue.put("dumping", lbp.getDumping());
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
    
    /**
     * Copy properties from a passed alg object.
     * @param propPane
     * @param alg
     */
    public void copyProperties(Inferencer alg) {
        Map<String, Object> propToValue = getInferencerProps(alg);
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof PropertyItemPane) {
                PropertyItemPane propItemPane = (PropertyItemPane) comp;
                Object value = propToValue.get(propItemPane.key);
                propItemPane.resetValue(value);
            }
        }
    }
    
    /**
     * Commit changed values into the wrapped Inferencer in the object.
     * @return false if an error occurs during committing.
     */
    public boolean commitValues() {
        Map<String, Object> keyToValue = new HashMap<String, Object>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof PropertyItemPane) {
                PropertyItemPane propPane = (PropertyItemPane) comp;
                if (!propPane.assignPropertyValue()) 
                    return false;
                keyToValue.put(propPane.key,
                               propPane.value);
            }
        }
        if (!commitValues(keyToValue, algorithm))
            return false;
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
