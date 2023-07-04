/*
 * Created on Apr 8, 2008
 *
 */
package org.reactome.funcInt;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to reference to an entry in an external database. For example,
 * a protein can be linked to a gene id in Entrez.
 * @author wgm
 *
 */
@XmlRootElement
public class DbReference {
    
    private long dbId;
    private String accession;
    private String dbName;
    
    public DbReference() {
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    
}
