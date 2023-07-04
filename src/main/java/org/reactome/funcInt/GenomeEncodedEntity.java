/*
 * Created on Jan 20, 2009
 *
 */
package org.reactome.funcInt;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * This abstract class is used to supclass for Protein, Gene, mRNA. This mainly is used for 
 * organization purpose.
 * @author wgm
 *
 */
@XmlRootElement
public abstract class GenomeEncodedEntity {
    
    private long dbId;
    // Primary external database reference. A primary database for a protein
    // should be UniProt usually.
    private DbReference primaryDbReference;
    // Amino acid sequence information
    private String sequence;
    // Used to fast search sequence
    private String checkSum;
    // DbReference properties are used to map to other database ids
    private Set<DbReference> dbReferences;
    
    public void setCheckSum(String checksum) {
        this.checkSum = checksum;
    }
    
    public String getCheckSum() {
        return this.checkSum;
    }
    
    public void setSequence(String aa) {
        this.sequence = aa;
    }
    
    @XmlTransient // Don't want these sequences to be returned in the client
    public String getSequence() {
        return this.sequence;
    }

    public void setDbReferences(Set<DbReference> xrefs) {
        this.dbReferences = xrefs;
    }
    
    public Set<DbReference> getDbReferences() {
        return this.dbReferences;
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
     * Set the primary external database accession number.
     * @param accession
     */
    public void setPrimaryAccession(String accession) {
        DbReference dbRef = getPrimaryDbReference();
        if (dbRef != null)
            dbRef.setAccession(accession);
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public void setPrimaryDbReference(DbReference dbReference) {
        this.primaryDbReference = dbReference;
    }
    
    public DbReference getPrimaryDbReference() {
        // This method is basically used as a container for the primary
        // database and accession
        if (primaryDbReference == null)
            primaryDbReference = new DbReference();
        return this.primaryDbReference;
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof GenomeEncodedEntity))
            return false;
        GenomeEncodedEntity out = (GenomeEncodedEntity) other;
        return out.getCheckSum().equals(getCheckSum());
    }
    
    public int hashCode() {
        String checksum = getCheckSum();
        if (checksum != null)
            return checksum.hashCode();
        return super.hashCode();
    }
}
