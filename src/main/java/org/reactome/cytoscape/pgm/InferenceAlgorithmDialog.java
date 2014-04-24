/*
 * Created on Apr 19, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.PGMInfAlgManager;
import org.reactome.pgm.PGMInferenceAlgorithm;
import org.reactome.pgm.PGMInferenceAlgorithm.PropertyItem;

/**
 * This customized JDialog is used to set up an inference algorithm for factor graphs.
 * @author gwu
 *
 */
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
    public PGMInferenceAlgorithm getSelectedAlgorithm() {
        if (isOkClicked)
            return (PGMInferenceAlgorithm) algBox.getSelectedItem();
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
        List<PGMInferenceAlgorithm> algorithms = PGMInfAlgManager.getManager().getAlgorithms();
        JLabel algLabel = new JLabel("Choose an inference algorithm:");
        algBox = new JComboBox();
        algBox.setEditable(false);
        for (PGMInferenceAlgorithm alg : algorithms) {
            // Don't make a copy in order to keep changes stuck in memory.
            algBox.addItem(alg);
        }
        algBox.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseAlgorithm((PGMInferenceAlgorithm)algBox.getSelectedItem());
            }
            
        });
        JPanel panel = new JPanel();
        panel.add(algLabel);
        panel.add(algBox);
        // Choose the first one as the default
        chooseAlgorithm((PGMInferenceAlgorithm)algBox.getItemAt(0));
        return panel;
    }
    
    private void chooseAlgorithm(PGMInferenceAlgorithm alg) {
        propertyPane.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = -1;
        for (PropertyItem item : alg.getPropertyItems()) {
            if (!item.isEditable())
                continue;
            PropertyItemPane itemPane = new PropertyItemPane();
            itemPane.setPropertyItem(item);
            constraints.gridy ++;
            propertyPane.add(itemPane, constraints);
        }
        propertyPane.invalidate();
        propertyPane.repaint();
        propertyPane.getParent().validate();
    }
    
    private boolean commitValues() {
        for (int i = 0; i < propertyPane.getComponentCount(); i++) {
            Component comp = propertyPane.getComponent(i);
            if (comp instanceof PropertyItemPane) {
                PropertyItemPane propPane = (PropertyItemPane) comp;
                if (!propPane.assignPropertyValue())
                    return false;
            }
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
        // View the detailed information about the algorithm from the libdai page
        JButton helpBtn = new JButton("Help");
        helpBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                PGMInferenceAlgorithm alg = (PGMInferenceAlgorithm) algBox.getSelectedItem();
                if (alg.getUrl() == null) {
                    JOptionPane.showMessageDialog(InferenceAlgorithmDialog.this,
                                                  "No URL for help is defined for the selected algorithm, " + alg.getAlgorithm() + ".",
                                                  "No Help",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PlugInUtilities.openURL(alg.getUrl());
            }
        });
        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        controlPane.add(resetBtn);
        controlPane.add(helpBtn);
        
        // Set okBtn as the default
        okBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(okBtn);
        
        return controlPane;
    }
    
    private void reset() {
        PGMInferenceAlgorithm alg = (PGMInferenceAlgorithm) algBox.getSelectedItem();
        // Get the original copy
        PGMInferenceAlgorithm original = PGMInfAlgManager.getManager().getAlgorithm(alg.getAlgorithm());
        Map<String, String> originalProps = original.getProperties();
        alg.setProperties(originalProps);
        // Reset the GUIs
        chooseAlgorithm(alg);
    }
    
    /**
     * A customized JPanel to display a simple PropertyItem.
     * @author gwu
     *
     */
    private class PropertyItemPane extends JPanel {
        private PropertyItem propertyItem;
        // One of the following should be used.
        // But not both.
        private JTextField valueTF;
        private JComboBox valueBox;
        
        public PropertyItemPane() {
        }
        
        public void setPropertyItem(PropertyItem item) {
            this.propertyItem = item;
            setUpGUIs();
        }
        
        private void setUpGUIs() {
            String label = propertyItem.getName();
            JLabel nameLabel = new JLabel(label);
            add(nameLabel);
            if (propertyItem.getType() == Boolean.class) {
                // A special case
                valueBox = new JComboBox();
                valueBox.addItem("false");
                valueBox.addItem("true");
                if (propertyItem.getValue().equals("0"))
                    valueBox.setSelectedItem("false");
                else if (propertyItem.getValue().equals("1"))
                    valueBox.setSelectedItem("true");
                add(valueBox);
            }
            else if (propertyItem.getChoices() != null && propertyItem.getChoices().size() > 0) {
                // Set up a JComboBox
                valueBox = new JComboBox();
                valueBox.setEditable(false);
                for (String choice : propertyItem.getChoices())
                    valueBox.addItem(choice);
                valueBox.setSelectedItem(propertyItem.getValue());
                add(valueBox);
            }
            else {
                valueTF = new JTextField(propertyItem.getValue());
                valueTF.setColumns(4);
                add(valueTF);
            }
            if (propertyItem.getUnit() != null) {
                label = "(" + propertyItem.getUnit() + ")";
                JLabel unitLabel = new JLabel(label);
                add(unitLabel);
            }
        }
        
        public boolean assignPropertyValue() {
            if (!validValue())
                return false;
            String value = null;
            if (valueTF == null)
                value = valueBox.getSelectedItem().toString();
            else 
                value = valueTF.getText().trim();
            // A special case
            if (propertyItem.getType() == Boolean.class) {
                if (value.equals("true"))
                    value = "1";
                else
                    value = "0";
            }
            propertyItem.setValue(value);
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
            String propName = propertyItem.getName();
            if (text.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "Value for " + propName + " should not be empty.",
                                              "Empty Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (propertyItem.getType() == Integer.class) {
                try {
                    Integer value = new Integer(text);
                    if (propertyItem.getMaxValue() != null && value > propertyItem.getMaxValue()) {
                        // Cast a double value to integer to avoid confusing
                        Integer maxValue = (int) propertyItem.getMaxValue().doubleValue();
                        JOptionPane.showMessageDialog(this,
                                                      "Value for " + propName + " should not exceed " + maxValue + ".",
                                                      "Invalid Value",
                                                      JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                catch(NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Value for " + propertyItem.getName() + " should be an integer.",
                                                  "Integer Required",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            else if (propertyItem.getType() == Double.class) {
                try {
                    Double value = new Double(text);
                    if (propertyItem.getMaxValue() != null && value > propertyItem.getMaxValue()) {
                        JOptionPane.showMessageDialog(this,
                                                      "Value for " + propName + " should not exceed " + propertyItem.getMaxValue() + ".",
                                                      "Invalid Value",
                                                      JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                catch(NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Value for " + propertyItem.getName() + " should ne a number.",
                                                  "Number equired",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            return true;
        }
    }
}
