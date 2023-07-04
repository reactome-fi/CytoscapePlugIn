/*
 * Created on Sep 28, 2006
 *
 */
package org.reactome.funcInt;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to describe evidence for predicted functional interactions.
 * @author guanming
 *
 */
@XmlRootElement
public class Evidence {
    private long dbId;
    // We may use an enum-based way to keep Evidence for predicted FIs
    // Using the following way to make it compatible with old NBC based features
    // for ReactomeFIVIz sake.
    private Boolean humanPPI;
    private Boolean mousePPI;
    private Boolean flyPPI;
    private Boolean wormPPI;
    private Boolean yeastPPI;
    private Boolean domainInteractions;
    private Boolean gOBPSharing;
    private Boolean achilles;
    private Boolean encodetf;
    private Boolean encodetfppi;
    private Boolean gadhighlevel;
    private Boolean generif;
    private Boolean genesigdb;
    private Boolean gobp;
    private Boolean gomf;
    private Boolean hpo;
    private Boolean hubs;
    private Boolean hugenavigator;
    private Boolean jensencompartmenttextmining;
    private Boolean jensendiseasecurated;
    private Boolean jensendiseasetextmining;
    private Boolean jensentissuetextmining;
    private Boolean lincscmapchemical;
    private Boolean msigdbcomp;
    private Boolean nursa;
    private Boolean phosphositetextmining;
    private Boolean proteomicsdb;
    private Boolean GTEx_Adipose_Subcutaneous;
    private Boolean GTEx_Adipose_Visceral_Omentum;
    private Boolean GTEx_AdrenalGland;
    private Boolean GTEx_Artery_Aorta;
    private Boolean GTEx_Artery_Coronary;
    private Boolean GTEx_Artery_Tibial;
    private Boolean GTEx_Brain_Amygdala;
    private Boolean GTEx_Brain_Caudate_basalganglia;
    private Boolean GTEx_Brain_CerebellarHemisphere;
    private Boolean GTEx_Brain_Cerebellum;
    private Boolean GTEx_Brain_Cortex;
    private Boolean GTEx_Brain_FrontalCortex_BA9;
    private Boolean GTEx_Brain_Hippocampus;
    private Boolean GTEx_Brain_Hypothalamus;
    private Boolean GTEx_Brain_Nucleusaccumbens_basalganglia;
    private Boolean GTEx_Brain_Spinalcord_cervicalc_1;
    private Boolean GTEx_Brain_Substantianigra;
    private Boolean GTEx_Breast_MammaryTissue;
    private Boolean GTEx_Cells_Culturedfibroblasts;
    private Boolean GTEx_Cells_EBV_transformedlymphocytes;
    private Boolean GTEx_Colon_Sigmoid;
    private Boolean GTEx_Colon_Transverse;
    private Boolean GTEx_Esophagus_GastroesophagealJunction;
    private Boolean GTEx_Esophagus_Mucosa;
    private Boolean GTEx_Esophagus_Muscularis;
    private Boolean GTEx_Heart_AtrialAppendage;
    private Boolean GTEx_Heart_LeftVentricle;
    private Boolean GTEx_Kidney_Cortex;
    private Boolean GTEx_Liver;
    private Boolean GTEx_Lung;
    private Boolean GTEx_MinorSalivaryGland;
    private Boolean GTEx_Muscle_Skeletal;
    private Boolean GTEx_Nerve_Tibial;
    private Boolean GTEx_Ovary;
    private Boolean GTEx_Pancreas;
    private Boolean GTEx_Pituitary;
    private Boolean GTEx_Prostate;
    private Boolean GTEx_Skin_All;
    private Boolean GTEx_Skin_NotSunExposed_Suprapubic;
    private Boolean GTEx_Skin_SunExposed_Lowerleg;
    private Boolean GTEx_SmallIntestine_TerminalIleum;
    private Boolean GTEx_Spleen;
    private Boolean GTEx_Stomach;
    private Boolean GTEx_Testis;
    private Boolean GTEx_Thyroid;
    private Boolean GTEx_Uterus;
    private Boolean GTEx_Vagina;
    private Boolean GTEx_WholeBlood;
    private Boolean TCGA_ACC;
    private Boolean TCGA_BLCA;
    private Boolean TCGA_BRCA;
    private Boolean TCGA_CESC;
    private Boolean TCGA_CHOL;
    private Boolean TCGA_COAD;
    private Boolean TCGA_DLBC;
    private Boolean TCGA_ESCA;
    private Boolean TCGA_GBM;
    private Boolean TCGA_HNSC;
    private Boolean TCGA_KICH;
    private Boolean TCGA_KIRC;
    private Boolean TCGA_KIRP;
    private Boolean TCGA_LGG;
    private Boolean TCGA_LIHC;
    private Boolean TCGA_LUAD;
    private Boolean TCGA_LUSC;
    private Boolean TCGA_MESO;
    private Boolean TCGA_OV;
    private Boolean TCGA_PAAD;
    private Boolean TCGA_PCPG;
    private Boolean TCGA_PRAD;
    private Boolean TCGA_READ;
    private Boolean TCGA_SARC;
    private Boolean TCGA_SKCM;
    private Boolean TCGA_STAD;
    private Boolean TCGA_TGCT;
    private Boolean TCGA_THCA;
    private Boolean TCGA_THYM;
    private Boolean TCGA_UCEC;
    private Boolean TCGA_UCS;
    
    // NBC-based properties
    private Boolean humanInteraction;
    private Boolean dmePPI;
    private Boolean celPPI;
    private Boolean scePPI;
    private Boolean pfamDomainInt;
    private Boolean goBPSharing;
    private Boolean pavlidisGeneExp;
    private Boolean carlosGeneExp;
    // Probability for true functional interaction
    private double probability;

    // Probability for true functional interaction
    private double score;
    
    public Evidence() {
    }
    
    public Boolean getHumanInteraction() {
        return humanInteraction;
    }

    public void setHumanInteraction(Boolean humanInteraction) {
        this.humanInteraction = humanInteraction;
    }

    public Boolean getDmePPI() {
        return dmePPI;
    }



    public void setDmePPI(Boolean dmePPI) {
        this.dmePPI = dmePPI;
    }



    public Boolean getCelPPI() {
        return celPPI;
    }



    public void setCelPPI(Boolean celPPI) {
        this.celPPI = celPPI;
    }



    public Boolean getScePPI() {
        return scePPI;
    }



    public void setScePPI(Boolean scePPI) {
        this.scePPI = scePPI;
    }



    public Boolean getPfamDomainInt() {
        return pfamDomainInt;
    }



    public void setPfamDomainInt(Boolean pfamDomainInt) {
        this.pfamDomainInt = pfamDomainInt;
    }



    public Boolean getGoBPSharing() {
        return goBPSharing;
    }



    public void setGoBPSharing(Boolean goBPSharing) {
        this.goBPSharing = goBPSharing;
    }



    public Boolean getPavlidisGeneExp() {
        return pavlidisGeneExp;
    }



    public void setPavlidisGeneExp(Boolean pavlidisGeneExp) {
        this.pavlidisGeneExp = pavlidisGeneExp;
    }



    public Boolean getCarlosGeneExp() {
        return carlosGeneExp;
    }



    public void setCarlosGeneExp(Boolean carlosGeneExp) {
        this.carlosGeneExp = carlosGeneExp;
    }



    public double getProbability() {
        return probability;
    }



    public void setProbability(double probability) {
        this.probability = probability;
    }



    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public Boolean getHumanPPI() {
        return humanPPI;
    }

    public void setHumanPPI(Boolean humanPPI) {
        this.humanPPI = humanPPI;
    }

    public Boolean getMousePPI() {
        return mousePPI;
    }

    public void setMousePPI(Boolean mousePPI) {
        this.mousePPI = mousePPI;
    }

    public Boolean getFlyPPI() {
        return flyPPI;
    }

    public void setFlyPPI(Boolean flyPPI) {
        this.flyPPI = flyPPI;
    }

    public Boolean getWormPPI() {
        return wormPPI;
    }

    public void setWormPPI(Boolean wormPPI) {
        this.wormPPI = wormPPI;
    }

    public Boolean getYeastPPI() {
        return yeastPPI;
    }

    public void setYeastPPI(Boolean yeastPPI) {
        this.yeastPPI = yeastPPI;
    }

    public Boolean getDomainInteractions() {
        return domainInteractions;
    }

    public void setDomainInteractions(Boolean domainInteractions) {
        this.domainInteractions = domainInteractions;
    }

    public Boolean getgOBPSharing() {
        return gOBPSharing;
    }

    public void setgOBPSharing(Boolean gOBPSharing) {
        this.gOBPSharing = gOBPSharing;
    }

    public Boolean getAchilles() {
        return achilles;
    }

    public void setAchilles(Boolean achilles) {
        this.achilles = achilles;
    }

    public Boolean getEncodetf() {
        return encodetf;
    }

    public void setEncodetf(Boolean encodetf) {
        this.encodetf = encodetf;
    }

    public Boolean getEncodetfppi() {
        return encodetfppi;
    }

    public void setEncodetfppi(Boolean encodetfppi) {
        this.encodetfppi = encodetfppi;
    }

    public Boolean getGadhighlevel() {
        return gadhighlevel;
    }

    public void setGadhighlevel(Boolean gadhighlevel) {
        this.gadhighlevel = gadhighlevel;
    }

    public Boolean getGenerif() {
        return generif;
    }

    public void setGenerif(Boolean generif) {
        this.generif = generif;
    }

    public Boolean getGenesigdb() {
        return genesigdb;
    }

    public void setGenesigdb(Boolean genesigdb) {
        this.genesigdb = genesigdb;
    }

    public Boolean getGobp() {
        return gobp;
    }

    public void setGobp(Boolean gobp) {
        this.gobp = gobp;
    }

    public Boolean getGomf() {
        return gomf;
    }

    public void setGomf(Boolean gomf) {
        this.gomf = gomf;
    }

    public Boolean getHpo() {
        return hpo;
    }

    public void setHpo(Boolean hpo) {
        this.hpo = hpo;
    }

    public Boolean getHubs() {
        return hubs;
    }

    public void setHubs(Boolean hubs) {
        this.hubs = hubs;
    }

    public Boolean getHugenavigator() {
        return hugenavigator;
    }

    public void setHugenavigator(Boolean hugenavigator) {
        this.hugenavigator = hugenavigator;
    }

    public Boolean getJensencompartmenttextmining() {
        return jensencompartmenttextmining;
    }

    public void setJensencompartmenttextmining(Boolean jensencompartmenttextmining) {
        this.jensencompartmenttextmining = jensencompartmenttextmining;
    }

    public Boolean getJensendiseasecurated() {
        return jensendiseasecurated;
    }

    public void setJensendiseasecurated(Boolean jensendiseasecurated) {
        this.jensendiseasecurated = jensendiseasecurated;
    }

    public Boolean getJensendiseasetextmining() {
        return jensendiseasetextmining;
    }

    public void setJensendiseasetextmining(Boolean jensendiseasetextmining) {
        this.jensendiseasetextmining = jensendiseasetextmining;
    }

    public Boolean getJensentissuetextmining() {
        return jensentissuetextmining;
    }

    public void setJensentissuetextmining(Boolean jensentissuetextmining) {
        this.jensentissuetextmining = jensentissuetextmining;
    }

    public Boolean getLincscmapchemical() {
        return lincscmapchemical;
    }

    public void setLincscmapchemical(Boolean lincscmapchemical) {
        this.lincscmapchemical = lincscmapchemical;
    }

    public Boolean getMsigdbcomp() {
        return msigdbcomp;
    }

    public void setMsigdbcomp(Boolean msigdbcomp) {
        this.msigdbcomp = msigdbcomp;
    }

    public Boolean getNursa() {
        return nursa;
    }

    public void setNursa(Boolean nursa) {
        this.nursa = nursa;
    }

    public Boolean getPhosphositetextmining() {
        return phosphositetextmining;
    }

    public void setPhosphositetextmining(Boolean phosphositetextmining) {
        this.phosphositetextmining = phosphositetextmining;
    }

    public Boolean getProteomicsdb() {
        return proteomicsdb;
    }

    public void setProteomicsdb(Boolean proteomicsdb) {
        this.proteomicsdb = proteomicsdb;
    }

    public Boolean getGTEx_Adipose_Subcutaneous() {
        return GTEx_Adipose_Subcutaneous;
    }

    public void setGTEx_Adipose_Subcutaneous(Boolean gTEx_Adipose_Subcutaneous) {
        GTEx_Adipose_Subcutaneous = gTEx_Adipose_Subcutaneous;
    }

    public Boolean getGTEx_Adipose_Visceral_Omentum() {
        return GTEx_Adipose_Visceral_Omentum;
    }

    public void setGTEx_Adipose_Visceral_Omentum(Boolean gTEx_Adipose_Visceral_Omentum) {
        GTEx_Adipose_Visceral_Omentum = gTEx_Adipose_Visceral_Omentum;
    }

    public Boolean getGTEx_AdrenalGland() {
        return GTEx_AdrenalGland;
    }

    public void setGTEx_AdrenalGland(Boolean gTEx_AdrenalGland) {
        GTEx_AdrenalGland = gTEx_AdrenalGland;
    }

    public Boolean getGTEx_Artery_Aorta() {
        return GTEx_Artery_Aorta;
    }

    public void setGTEx_Artery_Aorta(Boolean gTEx_Artery_Aorta) {
        GTEx_Artery_Aorta = gTEx_Artery_Aorta;
    }

    public Boolean getGTEx_Artery_Coronary() {
        return GTEx_Artery_Coronary;
    }

    public void setGTEx_Artery_Coronary(Boolean gTEx_Artery_Coronary) {
        GTEx_Artery_Coronary = gTEx_Artery_Coronary;
    }

    public Boolean getGTEx_Artery_Tibial() {
        return GTEx_Artery_Tibial;
    }

    public void setGTEx_Artery_Tibial(Boolean gTEx_Artery_Tibial) {
        GTEx_Artery_Tibial = gTEx_Artery_Tibial;
    }

    public Boolean getGTEx_Brain_Amygdala() {
        return GTEx_Brain_Amygdala;
    }

    public void setGTEx_Brain_Amygdala(Boolean gTEx_Brain_Amygdala) {
        GTEx_Brain_Amygdala = gTEx_Brain_Amygdala;
    }

    public Boolean getGTEx_Brain_Caudate_basalganglia() {
        return GTEx_Brain_Caudate_basalganglia;
    }

    public void setGTEx_Brain_Caudate_basalganglia(Boolean gTEx_Brain_Caudate_basalganglia) {
        GTEx_Brain_Caudate_basalganglia = gTEx_Brain_Caudate_basalganglia;
    }

    public Boolean getGTEx_Brain_CerebellarHemisphere() {
        return GTEx_Brain_CerebellarHemisphere;
    }

    public void setGTEx_Brain_CerebellarHemisphere(Boolean gTEx_Brain_CerebellarHemisphere) {
        GTEx_Brain_CerebellarHemisphere = gTEx_Brain_CerebellarHemisphere;
    }

    public Boolean getGTEx_Brain_Cerebellum() {
        return GTEx_Brain_Cerebellum;
    }

    public void setGTEx_Brain_Cerebellum(Boolean gTEx_Brain_Cerebellum) {
        GTEx_Brain_Cerebellum = gTEx_Brain_Cerebellum;
    }

    public Boolean getGTEx_Brain_Cortex() {
        return GTEx_Brain_Cortex;
    }

    public void setGTEx_Brain_Cortex(Boolean gTEx_Brain_Cortex) {
        GTEx_Brain_Cortex = gTEx_Brain_Cortex;
    }

    public Boolean getGTEx_Brain_FrontalCortex_BA9() {
        return GTEx_Brain_FrontalCortex_BA9;
    }

    public void setGTEx_Brain_FrontalCortex_BA9(Boolean gTEx_Brain_FrontalCortex_BA9) {
        GTEx_Brain_FrontalCortex_BA9 = gTEx_Brain_FrontalCortex_BA9;
    }

    public Boolean getGTEx_Brain_Hippocampus() {
        return GTEx_Brain_Hippocampus;
    }

    public void setGTEx_Brain_Hippocampus(Boolean gTEx_Brain_Hippocampus) {
        GTEx_Brain_Hippocampus = gTEx_Brain_Hippocampus;
    }

    public Boolean getGTEx_Brain_Hypothalamus() {
        return GTEx_Brain_Hypothalamus;
    }

    public void setGTEx_Brain_Hypothalamus(Boolean gTEx_Brain_Hypothalamus) {
        GTEx_Brain_Hypothalamus = gTEx_Brain_Hypothalamus;
    }

    public Boolean getGTEx_Brain_Nucleusaccumbens_basalganglia() {
        return GTEx_Brain_Nucleusaccumbens_basalganglia;
    }

    public void setGTEx_Brain_Nucleusaccumbens_basalganglia(Boolean gTEx_Brain_Nucleusaccumbens_basalganglia) {
        GTEx_Brain_Nucleusaccumbens_basalganglia = gTEx_Brain_Nucleusaccumbens_basalganglia;
    }

    public Boolean getGTEx_Brain_Spinalcord_cervicalc_1() {
        return GTEx_Brain_Spinalcord_cervicalc_1;
    }

    public void setGTEx_Brain_Spinalcord_cervicalc_1(Boolean gTEx_Brain_Spinalcord_cervicalc_1) {
        GTEx_Brain_Spinalcord_cervicalc_1 = gTEx_Brain_Spinalcord_cervicalc_1;
    }

    public Boolean getGTEx_Brain_Substantianigra() {
        return GTEx_Brain_Substantianigra;
    }

    public void setGTEx_Brain_Substantianigra(Boolean gTEx_Brain_Substantianigra) {
        GTEx_Brain_Substantianigra = gTEx_Brain_Substantianigra;
    }

    public Boolean getGTEx_Breast_MammaryTissue() {
        return GTEx_Breast_MammaryTissue;
    }

    public void setGTEx_Breast_MammaryTissue(Boolean gTEx_Breast_MammaryTissue) {
        GTEx_Breast_MammaryTissue = gTEx_Breast_MammaryTissue;
    }

    public Boolean getGTEx_Cells_Culturedfibroblasts() {
        return GTEx_Cells_Culturedfibroblasts;
    }

    public void setGTEx_Cells_Culturedfibroblasts(Boolean gTEx_Cells_Culturedfibroblasts) {
        GTEx_Cells_Culturedfibroblasts = gTEx_Cells_Culturedfibroblasts;
    }

    public Boolean getGTEx_Cells_EBV_transformedlymphocytes() {
        return GTEx_Cells_EBV_transformedlymphocytes;
    }

    public void setGTEx_Cells_EBV_transformedlymphocytes(Boolean gTEx_Cells_EBV_transformedlymphocytes) {
        GTEx_Cells_EBV_transformedlymphocytes = gTEx_Cells_EBV_transformedlymphocytes;
    }

    public Boolean getGTEx_Colon_Sigmoid() {
        return GTEx_Colon_Sigmoid;
    }

    public void setGTEx_Colon_Sigmoid(Boolean gTEx_Colon_Sigmoid) {
        GTEx_Colon_Sigmoid = gTEx_Colon_Sigmoid;
    }

    public Boolean getGTEx_Colon_Transverse() {
        return GTEx_Colon_Transverse;
    }

    public void setGTEx_Colon_Transverse(Boolean gTEx_Colon_Transverse) {
        GTEx_Colon_Transverse = gTEx_Colon_Transverse;
    }

    public Boolean getGTEx_Esophagus_GastroesophagealJunction() {
        return GTEx_Esophagus_GastroesophagealJunction;
    }

    public void setGTEx_Esophagus_GastroesophagealJunction(Boolean gTEx_Esophagus_GastroesophagealJunction) {
        GTEx_Esophagus_GastroesophagealJunction = gTEx_Esophagus_GastroesophagealJunction;
    }

    public Boolean getGTEx_Esophagus_Mucosa() {
        return GTEx_Esophagus_Mucosa;
    }

    public void setGTEx_Esophagus_Mucosa(Boolean gTEx_Esophagus_Mucosa) {
        GTEx_Esophagus_Mucosa = gTEx_Esophagus_Mucosa;
    }

    public Boolean getGTEx_Esophagus_Muscularis() {
        return GTEx_Esophagus_Muscularis;
    }

    public void setGTEx_Esophagus_Muscularis(Boolean gTEx_Esophagus_Muscularis) {
        GTEx_Esophagus_Muscularis = gTEx_Esophagus_Muscularis;
    }

    public Boolean getGTEx_Heart_AtrialAppendage() {
        return GTEx_Heart_AtrialAppendage;
    }

    public void setGTEx_Heart_AtrialAppendage(Boolean gTEx_Heart_AtrialAppendage) {
        GTEx_Heart_AtrialAppendage = gTEx_Heart_AtrialAppendage;
    }

    public Boolean getGTEx_Heart_LeftVentricle() {
        return GTEx_Heart_LeftVentricle;
    }

    public void setGTEx_Heart_LeftVentricle(Boolean gTEx_Heart_LeftVentricle) {
        GTEx_Heart_LeftVentricle = gTEx_Heart_LeftVentricle;
    }

    public Boolean getGTEx_Kidney_Cortex() {
        return GTEx_Kidney_Cortex;
    }

    public void setGTEx_Kidney_Cortex(Boolean gTEx_Kidney_Cortex) {
        GTEx_Kidney_Cortex = gTEx_Kidney_Cortex;
    }

    public Boolean getGTEx_Liver() {
        return GTEx_Liver;
    }

    public void setGTEx_Liver(Boolean gTEx_Liver) {
        GTEx_Liver = gTEx_Liver;
    }

    public Boolean getGTEx_Lung() {
        return GTEx_Lung;
    }

    public void setGTEx_Lung(Boolean gTEx_Lung) {
        GTEx_Lung = gTEx_Lung;
    }

    public Boolean getGTEx_MinorSalivaryGland() {
        return GTEx_MinorSalivaryGland;
    }

    public void setGTEx_MinorSalivaryGland(Boolean gTEx_MinorSalivaryGland) {
        GTEx_MinorSalivaryGland = gTEx_MinorSalivaryGland;
    }

    public Boolean getGTEx_Muscle_Skeletal() {
        return GTEx_Muscle_Skeletal;
    }

    public void setGTEx_Muscle_Skeletal(Boolean gTEx_Muscle_Skeletal) {
        GTEx_Muscle_Skeletal = gTEx_Muscle_Skeletal;
    }

    public Boolean getGTEx_Nerve_Tibial() {
        return GTEx_Nerve_Tibial;
    }

    public void setGTEx_Nerve_Tibial(Boolean gTEx_Nerve_Tibial) {
        GTEx_Nerve_Tibial = gTEx_Nerve_Tibial;
    }

    public Boolean getGTEx_Ovary() {
        return GTEx_Ovary;
    }

    public void setGTEx_Ovary(Boolean gTEx_Ovary) {
        GTEx_Ovary = gTEx_Ovary;
    }

    public Boolean getGTEx_Pancreas() {
        return GTEx_Pancreas;
    }

    public void setGTEx_Pancreas(Boolean gTEx_Pancreas) {
        GTEx_Pancreas = gTEx_Pancreas;
    }

    public Boolean getGTEx_Pituitary() {
        return GTEx_Pituitary;
    }

    public void setGTEx_Pituitary(Boolean gTEx_Pituitary) {
        GTEx_Pituitary = gTEx_Pituitary;
    }

    public Boolean getGTEx_Prostate() {
        return GTEx_Prostate;
    }

    public void setGTEx_Prostate(Boolean gTEx_Prostate) {
        GTEx_Prostate = gTEx_Prostate;
    }

    public Boolean getGTEx_Skin_All() {
        return GTEx_Skin_All;
    }

    public void setGTEx_Skin_All(Boolean gTEx_Skin_All) {
        GTEx_Skin_All = gTEx_Skin_All;
    }

    public Boolean getGTEx_Skin_NotSunExposed_Suprapubic() {
        return GTEx_Skin_NotSunExposed_Suprapubic;
    }

    public void setGTEx_Skin_NotSunExposed_Suprapubic(Boolean gTEx_Skin_NotSunExposed_Suprapubic) {
        GTEx_Skin_NotSunExposed_Suprapubic = gTEx_Skin_NotSunExposed_Suprapubic;
    }

    public Boolean getGTEx_Skin_SunExposed_Lowerleg() {
        return GTEx_Skin_SunExposed_Lowerleg;
    }

    public void setGTEx_Skin_SunExposed_Lowerleg(Boolean gTEx_Skin_SunExposed_Lowerleg) {
        GTEx_Skin_SunExposed_Lowerleg = gTEx_Skin_SunExposed_Lowerleg;
    }

    public Boolean getGTEx_SmallIntestine_TerminalIleum() {
        return GTEx_SmallIntestine_TerminalIleum;
    }

    public void setGTEx_SmallIntestine_TerminalIleum(Boolean gTEx_SmallIntestine_TerminalIleum) {
        GTEx_SmallIntestine_TerminalIleum = gTEx_SmallIntestine_TerminalIleum;
    }

    public Boolean getGTEx_Spleen() {
        return GTEx_Spleen;
    }

    public void setGTEx_Spleen(Boolean gTEx_Spleen) {
        GTEx_Spleen = gTEx_Spleen;
    }

    public Boolean getGTEx_Stomach() {
        return GTEx_Stomach;
    }

    public void setGTEx_Stomach(Boolean gTEx_Stomach) {
        GTEx_Stomach = gTEx_Stomach;
    }

    public Boolean getGTEx_Testis() {
        return GTEx_Testis;
    }

    public void setGTEx_Testis(Boolean gTEx_Testis) {
        GTEx_Testis = gTEx_Testis;
    }

    public Boolean getGTEx_Thyroid() {
        return GTEx_Thyroid;
    }

    public void setGTEx_Thyroid(Boolean gTEx_Thyroid) {
        GTEx_Thyroid = gTEx_Thyroid;
    }

    public Boolean getGTEx_Uterus() {
        return GTEx_Uterus;
    }

    public void setGTEx_Uterus(Boolean gTEx_Uterus) {
        GTEx_Uterus = gTEx_Uterus;
    }

    public Boolean getGTEx_Vagina() {
        return GTEx_Vagina;
    }

    public void setGTEx_Vagina(Boolean gTEx_Vagina) {
        GTEx_Vagina = gTEx_Vagina;
    }

    public Boolean getGTEx_WholeBlood() {
        return GTEx_WholeBlood;
    }

    public void setGTEx_WholeBlood(Boolean gTEx_WholeBlood) {
        GTEx_WholeBlood = gTEx_WholeBlood;
    }

    public Boolean getTCGA_ACC() {
        return TCGA_ACC;
    }

    public void setTCGA_ACC(Boolean tCGA_ACC) {
        TCGA_ACC = tCGA_ACC;
    }

    public Boolean getTCGA_BLCA() {
        return TCGA_BLCA;
    }

    public void setTCGA_BLCA(Boolean tCGA_BLCA) {
        TCGA_BLCA = tCGA_BLCA;
    }

    public Boolean getTCGA_BRCA() {
        return TCGA_BRCA;
    }

    public void setTCGA_BRCA(Boolean tCGA_BRCA) {
        TCGA_BRCA = tCGA_BRCA;
    }

    public Boolean getTCGA_CESC() {
        return TCGA_CESC;
    }

    public void setTCGA_CESC(Boolean tCGA_CESC) {
        TCGA_CESC = tCGA_CESC;
    }

    public Boolean getTCGA_CHOL() {
        return TCGA_CHOL;
    }

    public void setTCGA_CHOL(Boolean tCGA_CHOL) {
        TCGA_CHOL = tCGA_CHOL;
    }

    public Boolean getTCGA_COAD() {
        return TCGA_COAD;
    }

    public void setTCGA_COAD(Boolean tCGA_COAD) {
        TCGA_COAD = tCGA_COAD;
    }

    public Boolean getTCGA_DLBC() {
        return TCGA_DLBC;
    }

    public void setTCGA_DLBC(Boolean tCGA_DLBC) {
        TCGA_DLBC = tCGA_DLBC;
    }

    public Boolean getTCGA_ESCA() {
        return TCGA_ESCA;
    }

    public void setTCGA_ESCA(Boolean tCGA_ESCA) {
        TCGA_ESCA = tCGA_ESCA;
    }

    public Boolean getTCGA_GBM() {
        return TCGA_GBM;
    }

    public void setTCGA_GBM(Boolean tCGA_GBM) {
        TCGA_GBM = tCGA_GBM;
    }

    public Boolean getTCGA_HNSC() {
        return TCGA_HNSC;
    }

    public void setTCGA_HNSC(Boolean tCGA_HNSC) {
        TCGA_HNSC = tCGA_HNSC;
    }

    public Boolean getTCGA_KICH() {
        return TCGA_KICH;
    }

    public void setTCGA_KICH(Boolean tCGA_KICH) {
        TCGA_KICH = tCGA_KICH;
    }

    public Boolean getTCGA_KIRC() {
        return TCGA_KIRC;
    }

    public void setTCGA_KIRC(Boolean tCGA_KIRC) {
        TCGA_KIRC = tCGA_KIRC;
    }

    public Boolean getTCGA_KIRP() {
        return TCGA_KIRP;
    }

    public void setTCGA_KIRP(Boolean tCGA_KIRP) {
        TCGA_KIRP = tCGA_KIRP;
    }

    public Boolean getTCGA_LGG() {
        return TCGA_LGG;
    }

    public void setTCGA_LGG(Boolean tCGA_LGG) {
        TCGA_LGG = tCGA_LGG;
    }

    public Boolean getTCGA_LIHC() {
        return TCGA_LIHC;
    }

    public void setTCGA_LIHC(Boolean tCGA_LIHC) {
        TCGA_LIHC = tCGA_LIHC;
    }

    public Boolean getTCGA_LUAD() {
        return TCGA_LUAD;
    }

    public void setTCGA_LUAD(Boolean tCGA_LUAD) {
        TCGA_LUAD = tCGA_LUAD;
    }

    public Boolean getTCGA_LUSC() {
        return TCGA_LUSC;
    }

    public void setTCGA_LUSC(Boolean tCGA_LUSC) {
        TCGA_LUSC = tCGA_LUSC;
    }

    public Boolean getTCGA_MESO() {
        return TCGA_MESO;
    }

    public void setTCGA_MESO(Boolean tCGA_MESO) {
        TCGA_MESO = tCGA_MESO;
    }

    public Boolean getTCGA_OV() {
        return TCGA_OV;
    }

    public void setTCGA_OV(Boolean tCGA_OV) {
        TCGA_OV = tCGA_OV;
    }

    public Boolean getTCGA_PAAD() {
        return TCGA_PAAD;
    }

    public void setTCGA_PAAD(Boolean tCGA_PAAD) {
        TCGA_PAAD = tCGA_PAAD;
    }

    public Boolean getTCGA_PCPG() {
        return TCGA_PCPG;
    }

    public void setTCGA_PCPG(Boolean tCGA_PCPG) {
        TCGA_PCPG = tCGA_PCPG;
    }

    public Boolean getTCGA_PRAD() {
        return TCGA_PRAD;
    }

    public void setTCGA_PRAD(Boolean tCGA_PRAD) {
        TCGA_PRAD = tCGA_PRAD;
    }

    public Boolean getTCGA_READ() {
        return TCGA_READ;
    }

    public void setTCGA_READ(Boolean tCGA_READ) {
        TCGA_READ = tCGA_READ;
    }

    public Boolean getTCGA_SARC() {
        return TCGA_SARC;
    }

    public void setTCGA_SARC(Boolean tCGA_SARC) {
        TCGA_SARC = tCGA_SARC;
    }

    public Boolean getTCGA_SKCM() {
        return TCGA_SKCM;
    }

    public void setTCGA_SKCM(Boolean tCGA_SKCM) {
        TCGA_SKCM = tCGA_SKCM;
    }

    public Boolean getTCGA_STAD() {
        return TCGA_STAD;
    }

    public void setTCGA_STAD(Boolean tCGA_STAD) {
        TCGA_STAD = tCGA_STAD;
    }

    public Boolean getTCGA_TGCT() {
        return TCGA_TGCT;
    }

    public void setTCGA_TGCT(Boolean tCGA_TGCT) {
        TCGA_TGCT = tCGA_TGCT;
    }

    public Boolean getTCGA_THCA() {
        return TCGA_THCA;
    }

    public void setTCGA_THCA(Boolean tCGA_THCA) {
        TCGA_THCA = tCGA_THCA;
    }

    public Boolean getTCGA_THYM() {
        return TCGA_THYM;
    }

    public void setTCGA_THYM(Boolean tCGA_THYM) {
        TCGA_THYM = tCGA_THYM;
    }

    public Boolean getTCGA_UCEC() {
        return TCGA_UCEC;
    }

    public void setTCGA_UCEC(Boolean tCGA_UCEC) {
        TCGA_UCEC = tCGA_UCEC;
    }

    public Boolean getTCGA_UCS() {
        return TCGA_UCS;
    }

    public void setTCGA_UCS(Boolean tCGA_UCS) {
        TCGA_UCS = tCGA_UCS;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
    

}
