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
import org.gk.util.ProgressPane;

@SuppressWarnings("serial")
public class FIProgressBar extends ProgressPane
{
    JProgressBar progressBar;
    private CySwingApplication desktopApp;
    private String status = "";
    private boolean isVisible = true;
    
    public FIProgressBar()
    {

    }
    
}
