/*
 * Created on Apr 19, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;

import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;

/**
 * This customized JDialog is used to set up an inference algorithm for factor graphs.
 * @author gwu
 *
 */
@SuppressWarnings("rawtypes")
public class InferenceAlgorithmDialog extends JDialog {
    private boolean isOkClicked;
    // For changes based on selection
    private JPanel propertyPane;
    private JComboBox algBox;
    
    public InferenceAlgorithmDialog(JFrame parentFrame) {
        super(parentFrame);
        init();
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    /**
     * Get the selected algorithm. The client to this method should check if isOkClicked() returns
     * true. If isOkClicked() returns false, null will be returned to avoid an un-validated 
     * PGMInferenceAlgorithm object.
     * @return
     */
    public Inferencer getSelectedAlgorithm() {
        if (isOkClicked) {
            return (Inferencer) algBox.getSelectedItem();
        }
        return null;
    }
    
    private void init() {
        setTitle("Inference Algorithm Configuration");
        JPanel contentPane = createContentPane();
        JPanel controlPane = createControlPane();
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    private JPanel createContentPane() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        
        propertyPane = new JPanel();
        propertyPane.setBorder(BorderFactory.createEtchedBorder());
        propertyPane.setLayout(new GridBagLayout());
        contentPane.add(propertyPane, BorderLayout.CENTER);
        
        JPanel algListPane = createAlgListPane();
        Border border1 = BorderFactory.createEtchedBorder();
        Border border2 = BorderFactory.createEmptyBorder(4, 0, 4, 0);
        algListPane.setBorder(BorderFactory.createCompoundBorder(border1, border2));
        contentPane.add(algListPane, BorderLayout.NORTH);
        
        return contentPane;
    }
    
    private JPanel createAlgListPane() {
        JLabel algLabel = new JLabel("Choose an inference algorithm:");
        algBox = new JComboBox();
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                // Just want to display a simple name for inferrers
                return super.getListCellRendererComponent(list, value.getClass().getSimpleName(), index, isSelected,
                                                          cellHasFocus);
            }
        };
        algBox.setRenderer(renderer);
        algBox.setEditable(false);
        // There are only two algorithms are supported
        PathwayPGMConfiguration config = PathwayPGMConfiguration.getConfig();
        algBox.addItem(config.getLBP());
        algBox.addItem(config.getGibbsSampling());
        algBox.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseAlgorithm((Inferencer)algBox.getSelectedItem());
            }
            
        });
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        panel.add(algLabel, constraints);
        constraints.gridx = 1;
        panel.add(algBox, constraints);
        // Choose the first one as the default
        chooseAlgorithm((Inferencer)algBox.getItemAt(0));
        return panel;
    }
    
    private void chooseAlgorithm(Inferencer alg) {
        propertyPane.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = -1;
        Map<String, Object> keyToValue = getInferencerProps(alg);
        List<String> keys = new ArrayList<String>(keyToValue.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            PropertyItemPane itemPane = new PropertyItemPane();
            itemPane.setPropertyItem(key,
                                     keyToValue.get(key));
            constraints.gridy ++;
            propertyPane.add(itemPane, constraints);
        }
        propertyPane.invalidate();
        propertyPane.repaint();
        propertyPane.getParent().validate();
    }
    
    private Map<String, Object> getInferencerProps(Inferencer alg) {
        Map<String, Object> keyToValue = new HashMap<String, Object>();
        if (alg instanceof LoopyBeliefPropagation) {
            LoopyBeliefPropagation lbp = (LoopyBeliefPropagation) alg;
            keyToValue.put("maxIteration", lbp.getMaxIteration());
            keyToValue.put("tolerance", lbp.getTolerance());
            keyToValue.put("useLogSpace", lbp.getUseLogSpace());
            keyToValue.put("dumping", lbp.getDumping());
        }
        else if (alg instanceof GibbsSampling) {
            GibbsSampling gibbs = (GibbsSampling) alg;
            keyToValue.put("maxIteration", gibbs.getMaxIteration());
            keyToValue.put("burnin", gibbs.getBurnin());
            keyToValue.put("restart", gibbs.getRestart());
        }
        return keyToValue;
    }
    
    private boolean commitValues() {
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
        if (!commitValues(keyToValue))
            return false;
        return true;
    }
    
    private boolean commitValues(Map<String, Object> keyToValue) {
        Inferencer alg = (Inferencer) algBox.getSelectedItem();
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
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!commitValues())
                    return;
                isOkClicked = true;
                dispose();
            }
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isOkClicked = false;
                dispose();
            }
        });
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        controlPane.add(resetBtn);
        
        // Set okBtn as the default
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        
        return controlPane;
    }
    
    private void reset() {
        Inferencer alg = (Inferencer) algBox.getSelectedItem();
        // Reset the GUIs
        chooseAlgorithm(alg);
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
        private JComboBox valueBox;
        
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
                valueBox = new JComboBox();
                valueBox.addItem(Boolean.TRUE);
                valueBox.addItem(Boolean.FALSE);
                valueBox.setSelectedItem(value);
                add(valueBox);
            }
            else {
                valueTF = new JTextField(value + "");
                valueTF.setColumns(4);
                add(valueTF);
            }
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
