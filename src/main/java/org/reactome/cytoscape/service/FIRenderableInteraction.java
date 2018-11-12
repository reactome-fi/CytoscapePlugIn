/*
 * Created on Feb 18, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.gk.render.RenderableInteraction;
import org.gk.render.RendererFactory;

/**
 * A customized RenderableInteraction for FI in order to indicate
 * both directions based on FI annotations.
 * @author gwu
 *
 */
public class FIRenderableInteraction extends RenderableInteraction {
    // A static block so the following statement will be called once only
    static {
        FIRenderableInteractionRenderer renderer = new FIRenderableInteractionRenderer();
        RendererFactory.getFactory().registerRenderer(FIRenderableInteraction.class,
                                                      renderer);
    }
    
    // Direction in the input side
    private FIDirectionType inputDirection;
    // Direction in the output side
    private FIDirectionType outputDirection;
    
    /**
     * Default constructor.
     */
    public FIRenderableInteraction() {
        // Default directions
        inputDirection = FIDirectionType.NO;
        outputDirection = FIDirectionType.NO;
    }
    
    public FIDirectionType getInputDirection() {
        return inputDirection;
    }

    public void addInputDirection(FIDirectionType direction) {
        this.inputDirection = this.inputDirection.add(direction);
    }

    public FIDirectionType getOutputDirection() {
        return outputDirection;
    }

    public void addOutputDirection(FIDirectionType direction) {
        this.outputDirection = this.outputDirection.add(direction);
    }
    /**
     * Set directions for both input and output.
     * @param direction
     */
    public void setDirections(String direction) {
        FIDirectionType[] parsedDirections = parseDirections(direction);
        inputDirection = parsedDirections[0];
        outputDirection = parsedDirections[1];
    }
    
    private FIDirectionType[] parseDirections(String direction) {
        if (direction == null || direction.length() <= 1) // If the length == 1, the direction should be "-".
            return new FIDirectionType[]{FIDirectionType.NO, FIDirectionType.NO};
        FIDirectionType[] rtn = new FIDirectionType[2];
        char[] tokens = direction.toCharArray();
        if (tokens.length == 2) {
            if (tokens[0] == '-') {
                rtn[0] = FIDirectionType.NO;
                rtn[1] = FIDirectionType.parseChar(tokens[1]);
            }
            else {
                rtn[0] = FIDirectionType.parseChar(tokens[0]);
                rtn[1] = FIDirectionType.NO;
            }
        }
        else if (tokens.length == 3) {
            rtn[0] = FIDirectionType.parseChar(tokens[0]);
            rtn[1] = FIDirectionType.parseChar(tokens[2]);
        }
        return rtn;
    }
    
    /**
     * Add another FI direction to this object.
     * @param directions
     */
    public void addDirections(String directions) {
        FIDirectionType[] parsedDirections = parseDirections(directions);
        addInputDirection(parsedDirections[0]);
        addOutputDirection(parsedDirections[1]);
    }
    
    public JMenuItem createMenuItem() {
        JMenuItem rtn = null;
        String name = getDisplayName();
        // Check how many FIs have been merged in the specified interaction
        String[] tokens = name.split(", ");
        if (tokens.length > 1) {
            // Need to use submenus
            rtn = new JMenu("Query FI Source");
            // Do a sorting
            List<String> list = Arrays.asList(tokens);
            Collections.sort(list);
            for (String fi : list) {
                JMenuItem item = new JMenuItem(fi);
                final String tmpFi = fi;
                item.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        queryFISource(tmpFi);
                    }
                });
                rtn.add(item);
            }
        }
        else {
            rtn = new JMenuItem("Query FI Source");
            final String fi = tokens[0];
            rtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    queryFISource(fi);
                }
            });
        }
        return rtn;
    }
    
    private void queryFISource(String fi) {
        // Do a match
        Pattern pattern = Pattern.compile("(.+) - (.+)");
        Matcher matcher = pattern.matcher(fi);
        if (matcher.matches()) {
            String partner1 = matcher.group(1);
            String partner2 = matcher.group(2);
            FISourceQueryHelper queryHelper = new FISourceQueryHelper();
            queryHelper.queryFISource(partner1,
                                      partner2);
        }
    }

    static enum FIDirectionType {
        ARROW, // For activation
        T, // for inhibition
        NO; // for simple interaction
        
        public FIDirectionType add(FIDirectionType type) {
           // There are many combination here
            if (this == NO)
                return type;
            if (this == ARROW) {
                switch (type) {
                    case ARROW : return ARROW;
                    case T : return NO; // Conflict
                    case NO : return ARROW;
                }
            }
            if (this == T) {
                switch (type) {
                    case ARROW : return NO;
                    case T : return T;
                    case NO : return T;
                }
            }
           return NO; // Default as NO.
        }
        
        static FIDirectionType parseChar(char token) {
            if (token == '>' || token == '<')
                return ARROW;
            if (token == '|')
                return T;
            return NO;
        }
        
    }
    
}
