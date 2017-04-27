/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.osgi.framework.BundleContext;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.FuzzyLogicSimulator.ANDGateMode;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Impelmented as a CytoPanelComponent to be listed in the "Results Panel".
 * @author gwu
 *
 */
public class BooleanNetworkMainPane extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Boolean Network Modelling";
    private BooleanNetwork network;
    private PathwayEditor pathwayEditor;
    // To higlight pathway
    private PathwayHighlightControlPanel hiliteControlPane;
    // To hold samples
    private JTabbedPane tabbedPane;
    // Control buttons
    private JButton simulateBtn;
    private JButton newBtn;
    private JButton deleteBtn;
    private JButton compareBtn;
    // To avoid index error
    private boolean duringDeletion;
    
    /**
     * Default constructor.
     */
    public BooleanNetworkMainPane() {
        init();
    }
    
    private void initGUI() {
        setLayout(new BorderLayout());
        
        JPanel controlPane = createControlPane();
        add(controlPane, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!duringDeletion)
                    validateButtons();
            }
        });
        add(tabbedPane, BorderLayout.CENTER);
        
    }
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        
        simulateBtn = new JButton("Simulate");
        simulateBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                performSimulation();
            }
        });
        controlPane.add(simulateBtn);
        
        newBtn = new JButton("New");
        newBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewSimulation();
            }
        });
        controlPane.add(newBtn);
        
        deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSimulation();
            }
        });
        controlPane.add(deleteBtn);
        
        compareBtn = new JButton("Compare");
        compareBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                compareSimulation();
            }
        });
        controlPane.add(compareBtn);
        
        return controlPane;
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }

    private void compareSimulation() {
        CompareSimulationDialog dialog = new CompareSimulationDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked)
            return;
        String[] simuationNames = dialog.getSelectedSimulations();
        // Get simulations based on name
        int index = tabbedPane.indexOfTab(simuationNames[0]);
        BooleanNetworkSamplePane pane1 = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(index);
        SimulationTableModel sim1 = pane1.getSimulation();
        index = tabbedPane.indexOfTab(simuationNames[1]);
        BooleanNetworkSamplePane pane2 = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(index);
        SimulationTableModel sim2 = pane2.getSimulation();
        
        displayComparison(sim1, sim2);
    }
    
    private void displayComparison(SimulationTableModel sim1,
                                   SimulationTableModel sim2) {
        SimulationComparisonPane comparisonPane = new SimulationComparisonPane(sim1.getSimulationName() + " vs. " + sim2.getSimulationName());
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(comparisonPane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(comparisonPane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        comparisonPane.setHiliteControlPane(hiliteControlPane);
        comparisonPane.setSimulations(sim1, sim2);
    }
    
    private void deleteSimulation() {
        BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
        if (samplePane == null)
            return;
        duringDeletion = true;
        samplePane.delete();
        duringDeletion = false;
        validateButtons();
    }
    
    /**
     * Create a new simulation.
     */
    public void createNewSimulation() {
        NewSimulationDialog dialog = new NewSimulationDialog();
        Set<String> usedNames = getUsedNames();
        dialog.usedNames = usedNames;
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        BooleanNetworkSamplePane samplePane = new BooleanNetworkSamplePane();
        // The following order is important to display selected set of variables
        samplePane.setPathwayEditor(pathwayEditor);
        samplePane.setHiliteControlPane(hiliteControlPane);
        // Make sure default value set is called before setBooleanNetwork.
        samplePane.setDefaultValue(dialog.getDefaultValue());
        samplePane.setSampleName(dialog.getSimulationName());
        samplePane.setAndGateMode(dialog.getAndGateMode());
        samplePane.setBooleanNetwork(this.network);
        tabbedPane.add(dialog.getSimulationName(), samplePane);
        tabbedPane.setSelectedComponent(samplePane); // Select the newly created one
        validateButtons();
    }

    private Set<String> getUsedNames() {
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
            usedNames.add(tabbedPane.getTitleAt(i));
        }
        return usedNames;
    }
    
    private void validateButtons() {
        deleteBtn.setEnabled(tabbedPane.getComponentCount() > 0);
        // Simulate button
        if (tabbedPane.getComponentCount() == 0)
            simulateBtn.setEnabled(false);
        else {
            BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
            simulateBtn.setEnabled(!samplePane.isSimulationPerformed());
        }
        // Compare button: At least two simulations
        if (tabbedPane.getComponentCount() < 2)
            compareBtn.setEnabled(false);
        else {
            int count = 0;
            for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(i);
                if (samplePane.isSimulationPerformed())
                    count ++;
            }
            compareBtn.setEnabled(count > 1);
        }
    }
    
    private void performSimulation() {
        BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
        samplePane.simulate();
        validateButtons();
    }
    
    private void init() {
        initGUI();
        PlugInUtilities.registerCytoPanelComponent(this);
        // Most likely SessionAboutToBeLoadedListener should be used in 3.1.0.
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                getParent().remove(BooleanNetworkMainPane.this);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener, 
                                null);
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    /**
     * Set the target BooleanNetwork to be simulated.
     * @param network
     */
    public void setBooleanNetwork(BooleanNetwork network) {
        this.network = network;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    /**
     * Return EAST so that it is displayed as a tab in the Results Panel.
     */
    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public String getTitle() {
        return this.TITLE;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
    
    private class CompareSimulationDialog extends JDialog {
        private boolean isOKClicked;
        private JComboBox<String> simulationIBox;
        private JComboBox<String> simulationIIBox;
        
        public CompareSimulationDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Compare Simulations");
            JPanel contentPane = createContentPane();
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!checkSelections())
                        return;
                    isOKClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(400, 225);
            setLocationRelativeTo(getOwner());
        }
        
        private boolean checkSelections() {
            String[] selections = getSelectedSimulations();
            if (selections[0].equals(selections[1])) {
                JOptionPane.showMessageDialog(this,
                                              "The selected simulations are the same. Please choose\n" +
                                              "two different simulations to copmare.",
                                              "Error in Selection", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        private JPanel createContentPane() {
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.anchor = GridBagConstraints.WEST;
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose simulations to compare:");
            constraints.gridwidth = 3;
            contentPane.add(label, constraints);
            label = new JLabel("Simulation I");
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            label = new JLabel("Simulation II");
            constraints.gridx = 2;
            contentPane.add(label, constraints);
            
            simulationIBox = createSimulationBox();
            constraints.gridx = 0;
            constraints.gridy = 2;
            simulationIBox.setSelectedIndex(0);
            contentPane.add(simulationIBox, constraints);
            label = new JLabel("vs.");
            constraints.gridx = 1;
            contentPane.add(label, constraints);
            simulationIIBox = createSimulationBox();
            constraints.gridx = 2;
            simulationIIBox.setSelectedIndex(1);
            contentPane.add(simulationIIBox, constraints);
            
            return contentPane;
        }
        
        private JComboBox<String> createSimulationBox() {
            Vector<String> items = new Vector<>();
            for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                items.add(title);
            }
            Collections.sort(items);
            JComboBox<String> box = new JComboBox<>(items);
            box.setEditable(false);
            return box;
        }
        
        public String[] getSelectedSimulations() {
            String[] rtn = new String[2];
            rtn[0] = (String) simulationIBox.getSelectedItem();
            rtn[1] = (String) simulationIIBox.getSelectedItem();
            return rtn;
        }
        
    }
    
    private class NewSimulationDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField nameTF;
        private JTextField defaultValueTF;
        private JComboBox<ANDGateMode> andGateBox;
        private Set<String> usedNames;
        
        public NewSimulationDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("New Simulation");
            
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(1, 1, 1, 1);
            constraints.anchor = GridBagConstraints.WEST;
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Enter a name for the simulation:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            contentPane.add(label, constraints);
            nameTF = new JTextField();
            nameTF.setColumns(20);
            constraints.gridy = 1;
            contentPane.add(nameTF, constraints);
            label = GKApplicationUtilities.createTitleLabel("Enter default input value between 0 and 1:");
            constraints.gridy = 2;
            contentPane.add(label, constraints);
            defaultValueTF = new JTextField();
            defaultValueTF.setText(1.0 + ""); // Use 1.0 as the default.
            constraints.gridy = 3;
            contentPane.add(defaultValueTF, constraints);
            defaultValueTF.setColumns(20);
            
            JPanel pane = createAndGatePane();
            constraints.gridy = 4;
            contentPane.add(pane, constraints);
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
        
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!checkValues())
                        return;
                    isOkClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(370, 250);
            setLocationRelativeTo(this.getOwner());
        }
        
        private JPanel createAndGatePane() {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel("Choose AND gate mode: ");
            andGateBox = new JComboBox<>(ANDGateMode.values());
            panel.add(label);
            panel.add(andGateBox);
            // Use PROD as the default
            andGateBox.setSelectedIndex(1);
            return panel;
        }
        
        private boolean checkValues() {
            String name = nameTF.getText().trim();
            if (name.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "The name field is empty!",
                                              "Empty Name",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            // Check if the name has been used
            if (usedNames != null && usedNames.contains(name)) {
                JOptionPane.showMessageDialog(this,
                                              "The entered name has been used. Try a new name.",
                                              "Duplicated Name",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            String value = defaultValueTF.getText().trim();
            if (value.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "The value field is empty. Enter a number between 0 and 1 (inclusively).",
                                              "Empty Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            Double number = null;
            try {
                number = new Double(value);
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Enter a number between 0 and 1 (inclusively) for the default value.",
                                              "Wrong Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (number > 1.0 || number < 0.0) {
                JOptionPane.showMessageDialog(this,
                                              "Enter a number between 0 and 1 (inclusively) for the default value.",
                                              "Wrong Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        public ANDGateMode getAndGateMode() {
            return (ANDGateMode) andGateBox.getSelectedItem();
        }
        
        public String getSimulationName() {
            return nameTF.getText().trim();
        }
        
        public double getDefaultValue() {
            return new Double(defaultValueTF.getText().trim());
        }
    }
}
