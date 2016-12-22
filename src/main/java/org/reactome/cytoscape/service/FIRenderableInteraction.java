/*
 * Created on Feb 18, 2014
 *
 */
package org.reactome.cytoscape.service;

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
