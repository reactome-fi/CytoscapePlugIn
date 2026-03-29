package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.cytoscape.application.swing.CytoPanelName;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.CloseTabIcon;

/**
 * Simple south-panel tab for displaying an LLM-generated enrichment summary.
 */
@SuppressWarnings("serial")
public class LLMSummaryResultPane extends javax.swing.JPanel implements org.cytoscape.application.swing.CytoPanelComponent {
    public static final String TITLE = "LLM Enrichment Summary";
    private ServiceRegistration serviceRegistration;

    private final JEditorPane textArea;
    // Text used to the label so that the close icon is not too lonely :-(
    private JLabel titleLabel;

    public LLMSummaryResultPane() {
        setLayout(new BorderLayout());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        // Top panel with Close button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 4));
        JButton closeButton = new JButton(new CloseTabIcon());
        closeButton.addActionListener(e -> close());
        topPanel.add(closeButton, BorderLayout.EAST);
        titleLabel = new JLabel(TITLE);
        topPanel.add(titleLabel, BorderLayout.WEST);
        

        add(topPanel, BorderLayout.NORTH);        
        
        textArea = new JEditorPane();
        textArea.setEditable(false);
        textArea.setContentType("text/html");
        textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }
    
    public void setLabelText(String text) {
        this.titleLabel.setText(text);
    }
    
    public void setServiceRegistration(ServiceRegistration serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }
    
    private void close() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
            return;
        }
        if (getParent() == null)
            return;
        //To have the rest of the network reappear after
        //closing the browser panel, uncomment the next two lines.
        //this.hideOtherNodesBox.setSelected(false);
        //showAllEdges();
        getParent().remove(this);
    }

    public void setSummary(String text) {
        textArea.setText(text == null ? "" : text);
        textArea.setCaretPosition(0);
    }

    @Override
    public java.awt.Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.SOUTH;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
}
