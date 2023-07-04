/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to describe functional interactions. These interactions can be
 * extracted from the annotated pathway databases, or predicated based on protein 
 * protein interactions.
 * @author guanming
 *
 */
@XmlRootElement
public class Interaction {
    
    private long dbId;
    // The id in the first protein should be smaller
    // that the one in the second protein
    private Protein firstProtein;
    private Protein secondProtein;
    // An optional data source. For functional interactions that are extracted from
    // pathway databases, this property should NOT be null.
    private Set<ReactomeSource> reactomeSources;
    // Evidence for predicted functional interaction. If an Interaction is predicted,
    // evidence should NOT be null.
    private Evidence evidence;
    // Annotation
    private FIAnnotation annotation;
    // customized id (e.g. edge ids used in Cytoscape)
    private String customId;
    
    public Interaction() {
    }
    
    public void setCustomId(String id) {
        this.customId = id;
    }
    
    public String getCustomId() {
        return this.customId;
    }
    
    public void setAnnotation(FIAnnotation annotation) {
        this.annotation = annotation;
    }
    
    public FIAnnotation getAnnotation() {
        return this.annotation;
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public Protein getFirstProtein() {
        return firstProtein;
    }

    public void setFirstProtein(Protein firstProtein) {
        this.firstProtein = firstProtein;
    }

    public Set<ReactomeSource> getReactomeSources() {
        return reactomeSources;
    }

    public void setReactomeSources(Set<ReactomeSource> reactomeSources) {
        this.reactomeSources = reactomeSources;
    }
    
    public void addReactomeSource(ReactomeSource source) {
        if (reactomeSources == null)
            reactomeSources = new HashSet<ReactomeSource>();
        reactomeSources.add(source);
    }

    public Protein getSecondProtein() {
        return secondProtein;
    }

    public void setSecondProtein(Protein secondProtein) {
        this.secondProtein = secondProtein;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Interaction))
            return false;
        Interaction other = (Interaction) obj;
        if (other.getFirstProtein().equals(firstProtein) &&
            other.getSecondProtein().equals(secondProtein))
            return true;
        return false;
    }
    
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("firstProtein:");
        builder.append(firstProtein == null ? "null" : 
            (firstProtein.getPrimaryDbName() + ":" + firstProtein.getPrimaryAccession()));
        builder.append("secondProtein:");
        builder.append(secondProtein == null ? "null" : 
            (secondProtein.getPrimaryDbName() + ":" + secondProtein.getPrimaryAccession()));
        return builder.toString().hashCode();
    }
    
}
