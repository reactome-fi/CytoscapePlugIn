/*
 * Created on Feb 6, 2015
 *
 */
package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized JPanel is used to highlight pathways based on some numeric values.
 * @author gwu
 *
 */
public class PathwayHighlightControlPanel extends JPanel {
    private JLabel minValueLabel;
    private JLabel maxValueLabel;
    private PathwayEditor pathwayEditor;
    private Map<String, Double> idToValue;
    
    /**
     * Default constructor.
     */
    public PathwayHighlightControlPanel() {
        init();
    }
    
    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(2, 0, 2, 0);
        double[] minMaxValue = PlugInObjectManager.getManager().getMinMaxColorValues();
        minValueLabel = new JLabel(minMaxValue[0] + "");
        maxValueLabel = new JLabel(minMaxValue[1] + "");
        ColorSpectrumPane colorPane = new ColorSpectrumPane();
        colorPane.setPreferredSize(new Dimension(257, 20));
        add(minValueLabel, constraints);
        constraints.gridx = 1;
        add(colorPane, constraints);
        constraints.gridx = 2;
        add(maxValueLabel, constraints);
        // Double click to change the values
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    MinMaxValueDialog dialog = new MinMaxValueDialog();
                    dialog.setMinMaxValues(minValueLabel.getText(),
                                           maxValueLabel.getText());
                    dialog.setModal(true);
                    dialog.setVisible(true);
                    if (dialog.isOkClikeced) {
                        resetMinMaxValues(dialog.getMinMaxValues());
                    }
                }
            }
        });
        setToolTipText("Double click to change the min/max values");
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    public Map<String, Double> getIdToValue() {
        return idToValue;
    }

    public void setIdToValue(Map<String, Double> idToValue) {
        this.idToValue = idToValue;
    }

    public void resetMinMaxValues(double[] minMaxValues) {
        // Record these values first
        PlugInObjectManager.getManager().setMinMaxColorValues(minMaxValues);
        minValueLabel.setText(minMaxValues[0] + "");
        maxValueLabel.setText(minMaxValues[1] + "");
        resetPathwayColors(minMaxValues);
    }
    
    private void resetPathwayColors(double[] minMaxValues) {
        if (pathwayEditor == null || idToValue == null)
            return;
        PathwayDiagramHighlighter highlighter = new PathwayDiagramHighlighter();
        highlighter.highlightELV((RenderablePathway)pathwayEditor.getRenderable(),
                                 idToValue,
                                 minMaxValues[0], 
                                 minMaxValues[1]);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        // Since there is no HIGHLIGHT ActionType, using SELECTION instead
        // to make repaint consistent.
        pathwayEditor.fireGraphEditorActionEvent(ActionType.SELECTION);
    }
    
    public void highlight() {
        double[] minMaxValues = PlugInObjectManager.getManager().getMinMaxColorValues();
        resetPathwayColors(minMaxValues);
    }
    
    private class ColorSpectrumPane extends JComponent {
        private int[] colors;
        
        public ColorSpectrumPane() {
            colors = new PathwayDiagramHighlighter().getColorSpetrum();
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // Color lines
            double step = (double) getWidth() / colors.length;
            double x = 0.0d;
            int height = getHeight();
            Graphics2D g2 = (Graphics2D) g;
            for (int i = 0; i < colors.length; i++) {
                Color color = new Color(colors[i]);
                g2.setPaint(color);
                Rectangle2D r = new Rectangle2D.Double(x, 0, step, height);
                g2.fill(r);
                x += step;
            }
        }
    }
    
    private class MinMaxValueDialog extends JDialog {
        private JTextField minTF, maxTF;
        private boolean isOkClikeced;
        
        public MinMaxValueDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            setTitle("Max/Min Values for Coloring");
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createEtchedBorder());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            JLabel label = new JLabel("<html><b><u>Enter Max/Min Values for Coloring</u></b></html>");
            constraints.gridwidth = 2;
            panel.add(label, constraints);
            
            label = new JLabel("Minimum Value:");
            minTF = new JTextField("-1.0"); // Just a number here
            minTF.setColumns(4);
            constraints.gridwidth = 1;
            constraints.gridy = 1;
            panel.add(label, constraints);
            panel.add(minTF, constraints);
            
            label = new JLabel("Maxmimum Value:");
            maxTF = new JTextField("1.0");
            maxTF.setColumns(4);
            constraints.gridwidth = 1;
            constraints.gridy = 2;
            panel.add(label, constraints);
            panel.add(maxTF, constraints);
            getContentPane().add(panel, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateValues()) {
                        isOkClikeced = true;
                        dispose();
                    }
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setLocationRelativeTo(getOwner());
            setSize(325, 250);
        }
        
        public void setMinMaxValues(String min, String max) {
            minTF.setText(min);
            maxTF.setText(max);
        }
        
        public double[] getMinMaxValues() {
            double min = new Double(minTF.getText().trim());
            double max = new Double(maxTF.getText().trim());
            return new double[]{min, max};
        }
        
        private boolean validateValues() {
            double min, max;
            try {
                min = new Double(minTF.getText().trim());
                max = new Double(maxTF.getText().trim());
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Error in parsing value: " + e.getMessage(),
                                              "Error in Values", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            // Make sure min value is less than max value
            if (min >= max) {
                JOptionPane.showMessageDialog(this,
                                              "Error in values: the min value is not smaller than the max value.",
                                              "Error in Values", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
    }
}
