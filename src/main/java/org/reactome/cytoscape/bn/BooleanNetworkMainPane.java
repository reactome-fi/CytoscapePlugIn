/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.gk.graphEditor.PathwayEditor;
import org.osgi.framework.BundleContext;
import org.reactome.booleannetwork.BooleanNetwork;
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
    // To hold samples
    private JTabbedPane tabbedPane;
    // Controls
    private JButton simulateBtn;
    
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
        return controlPane;
    }
    
    private void performSimulation() {
        BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
        samplePane.simulate();
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
    
    /**
     * Add a data sample as initial conditions for simulation.
     */
    public void addSample() {
        BooleanNetworkSamplePane samplePane = new BooleanNetworkSamplePane();
        // The following order is important to display selected set of variables
        samplePane.setPathwayEditor(pathwayEditor);
        samplePane.setBooleanNetwork(this.network);
        tabbedPane.add("Sample", samplePane);
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
    
}
