/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to describe the data source for functional interactions that
 * are extracted from Reactome.
 * @author guanming
 *
 */
@XmlRootElement
public class ReactomeSource {
    
    private long dbId;
    private long reactomeId;
    // database name
    private String dataSource; 
    private ReactomeSourceType sourceType;
    
    public ReactomeSource() {
        
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public long getReactomeId() {
        return reactomeId;
    }

    public void setReactomeId(long reactomeId) {
        this.reactomeId = reactomeId;
    }

    public ReactomeSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ReactomeSourceType sourceType) {
        this.sourceType = sourceType;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof ReactomeSource))
            return false;
        ReactomeSource other = (ReactomeSource) obj;
        return reactomeId == other.getReactomeId();
    }
    
    public int hashCode() {
        return (int) reactomeId;
    }
    
}
