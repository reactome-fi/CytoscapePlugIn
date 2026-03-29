package org.reactome.cytoscape.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;

/**
 * The class which generates the 'X' icon for the tabs. The constructor 
 * accepts an icon which is extra to the 'X' icon, so you can have tabs 
 * like in JBuilder. This value is null if no extra icon is required. 
 * @author Mr_Silly @jdc modified by wgm.
 * */
public class CloseTabIcon implements Icon {
    private int x_pos;
    private int y_pos;
    private int width;
    private int height;
    //private Icon fileIcon;
    
    public CloseTabIcon() {
        width = 14;
        height = 14;
    }
    public void paintIcon(Component c, Graphics g, int x, int y) {
        x_pos = x;
        y_pos = y;
        Color col = g.getColor();
        g.setColor(Color.black);
        int b = 4;
        g.drawLine(x + b - 1, y + b, x + width - b - 1, y + height - b);
        g.drawLine(x + b , y + b, x + width - b, y + height - b);
        g.drawLine(x + width - b - 1, y + b, x + b - 1, y + height - b);
        g.drawLine(x + width - b, y + b, x + b, y + height - b);
        g.setColor(col);
        //if (fileIcon != null) {
        //  fileIcon.paintIcon(c, g, x + width, y_p);
        //}
    }
    public int getIconWidth() {
        return width;
    }
    public int getIconHeight() {
        return height;
    }
    public Rectangle getBounds() {
        return new Rectangle(x_pos, y_pos, width, height);
    }
}
