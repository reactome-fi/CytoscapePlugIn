package org.reactome.cytoscape3;

import java.awt.Container;
import java.awt.Cursor;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.cytoscape.application.swing.CySwingApplication;

public class FIProgressBar extends JDialog
{
    JProgressBar progressBar;
    private CySwingApplication desktopApp;
    private String status = "";
    private boolean isVisible = true;
    
    public FIProgressBar(String title)
    {
        init(title);
    }
    
    private void init(String title)
    {
        setTitle(title);
        JPanel panel = new JPanel();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
        this.desktopApp = desktopApp;
        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        JPanel statusPanel = new JPanel();
        JPanel progPanel = new JPanel();
        JLabel statusLabel = new JLabel("Status: " + this.status);
        statusPanel.add(statusLabel);
        progPanel.add(progressBar);
        panel.add(statusPanel);
        panel.add(progPanel);
        add(panel);
        setSize(300, 150);
        setLocationRelativeTo(getOwner());
        setVisible(isVisible);

    }
    
    public void setProgress(int i)
    {
        progressBar.setValue(i);
        
    }
    
    public void setStatusMessage(String status)
    {
        this.status  = status;
    }
    
    public void setWaitCursor()
    {
        desktopApp.getJFrame().getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    
    public void unsetWaitCursor()
    {
        desktopApp.getJFrame().getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    public void hide()
    {
        this.isVisible = false;
    }
    public void unhide()
    {
        this.isVisible = true;
    }
}
