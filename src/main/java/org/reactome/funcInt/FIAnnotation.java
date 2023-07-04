/*
 * Created on Jun 17, 2010
 *
 */
package org.reactome.funcInt;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FIAnnotation {
    private String interactionId;
    private String annotation;
    private String reverseAnnotation;
    // Types of directions: ->, <-, -|, |-, <->, |-|, -
    private String direction;
    // score 
    private Double score;

    public FIAnnotation() {
    }
    
    public String getReverseAnnotation() {
        return reverseAnnotation;
    }

    public void setReverseAnnotation(String reverseType) {
        this.reverseAnnotation = reverseType;
    }
    
    public void setInteractionId(String id) {
        this.interactionId = id;
    }
    
    public String getInteractionId() {
        return this.interactionId;
    }

    public String getAnnotation() {
        return annotation;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setAnnotation(String type) {
        this.annotation = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public FIAnnotation generateReverseType() {
        if (reverseAnnotation == null)
            return null;
        FIAnnotation rtn = new FIAnnotation();
        rtn.annotation = reverseAnnotation;
        rtn.reverseAnnotation = annotation;
        rtn.direction = generateReverseDirection();
        rtn.score = score; // They should be the same
        return rtn;
    }

    private String generateReverseDirection() {
        if ("->".equals(direction))
            return "<-";
        else if ("<-".equals(direction))
            return "->";
        else if ("-|".equals(direction))
            return "|-";
        else if ("|-".equals(direction))
            return "-|";
        else
            return direction;
    }
    
    public FIAnnotation cloneType() {
        FIAnnotation clone = new FIAnnotation();
        clone.annotation = annotation;
        clone.reverseAnnotation = reverseAnnotation;
        clone.score = score;
        clone.direction = direction; 
        return clone;
    }
    
    public boolean equals(Object type) {
        if (!(type instanceof FIAnnotation))
            return false;
        FIAnnotation other = (FIAnnotation) type;
        return other.annotation.equals(this.annotation);
    }
    
    @Override
    public int hashCode() {
        return annotation.hashCode();
    }
}