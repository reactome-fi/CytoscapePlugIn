package org.reactome.cytoscape3;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;


@SuppressWarnings("serial")
public class BrowserPanel extends JPanel implements CytoPanelComponent
{
    private String TITLE;
    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.SOUTH;
    }

    @Override
    public Icon getIcon()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTitle()
    {
        return TITLE;
    }
    
}
