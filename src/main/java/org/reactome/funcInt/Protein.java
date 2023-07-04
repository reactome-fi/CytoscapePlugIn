/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to hold some essential information for proteins involved in
 * functional interactions.
 * @author guanming
 *
 */
@XmlRootElement
public class Protein extends GenomeEncodedEntity {
    
    // Protein name
    private String name;
    // Short name maybe gene name or other protein names.
    private String shortName;
    
    public Protein() {
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String geneName) {
        this.shortName = geneName;
    }
    
    /**
     * Get the primary external database accession number.
     * @return
     */
    public String getPrimaryAccession() {
        DbReference dbRef = getPrimaryDbReference();
        if (dbRef != null)
            return dbRef.getAccession();
        return null;
    }
    
    /**
     * @deprecated Use @see getPrimaryAccession()
     * @return
     */
    public String getAccession() {
        return getPrimaryAccession();
    }
    
    /**
     * @deprecated Use @see setPrimaryAccession(String)
     * @param accession
     */
    public void setAccession(String accession) {
        setPrimaryAccession(accession);
    }
    
    /**
     * @deprecated Use @see getPrimaryDbName()
     * @return
     */
    public String getDbName() {
        return getPrimaryDbName();
    }
    
    /**
     * @deprecated Use @see setPrimaryDbName(String)
     * @param dbName
     */
    public void setDbName(String dbName) {
        setPrimaryDbName(dbName);
    }

    /**
     * Set the primary external database accession number.
     * @param accession
     */
    public void setPrimaryAccession(String accession) {
        DbReference dbRef = getPrimaryDbReference();
        if (dbRef != null)
            dbRef.setAccession(accession);
    }

    /**
     * Set the primary database name.
     * @return
     */
    public String getPrimaryDbName() {
        DbReference dbRef = getPrimaryDbReference();
        if (dbRef != null)
            return dbRef.getDbName();
        return null;
    }

    /**
     * Get the primary database name.
     * @param dbName
     */
    public void setPrimaryDbName(String dbName) {
        DbReference dbRef = getPrimaryDbReference();
        if (dbRef != null)
            dbRef.setDbName(dbName);
    }
    
    public String getLabel() {
        if (shortName != null)
            return shortName;
        if (name != null)
            return name;
        return getPrimaryAccession();
    }
    
    public boolean equals(Object protein) {
        if (!(protein instanceof Protein))
            return false;
        Protein out = (Protein) protein;
        // Check dbName first
//        if (out.getDbName() == null && dbName != null)
//            return false;
//        if (dbName == null && out.getDbName() != null)
//            return false;
//        if (dbName != null && out.getDbName() != null &&
//            !dbName.equals(out.getDbName()))
//            return false;
//        if (accession == null && out.getAccession() != null)
//            return false;
//        if (accession != null && out.getAccession() == null)
//            return false;
//        if (accession != null && out.getAccession() != null &&
//            !accession.equals(out.getAccession()))
//            return false;
//        return true;
        return out.getCheckSum().equals(getCheckSum());
    }
    
}
