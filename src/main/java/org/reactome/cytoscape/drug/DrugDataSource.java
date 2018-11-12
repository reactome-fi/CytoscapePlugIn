package org.reactome.cytoscape.drug;

public enum DrugDataSource {
    Targetome,
    DrugCentral;
    
    public String getAccessUrl() {
        if (this == DrugCentral)
            return "http://drugcentral.org/drugcard/";
        return null;
    }
    
    public static DrugDataSource getDataSource(String dbName) {
        if (dbName.equalsIgnoreCase(Targetome.toString()))
            return DrugDataSource.Targetome;
        if (dbName.equalsIgnoreCase(DrugCentral.toString()))
            return DrugDataSource.DrugCentral;
        return null;
    }

}
