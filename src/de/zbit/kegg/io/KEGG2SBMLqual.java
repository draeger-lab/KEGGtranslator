package de.zbit.kegg.io;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.test.gui.JSBMLvisualizer;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.util.Utils;

public class KEGG2SBMLqual extends KEGG2jSBML {
  public static final String QUAL_NS = "http://www.sbml.org/sbml/level3/version1/qual/version1";
  
  public static final String QUAL_NS_PREFIX = "qual";
  
  
  public QualitativeModel qualModel = null;
  
  
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * Initialize a new KEGG2SBMLqual object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2SBMLqual() {
    this(new KeggInfoManagement());
  }
  
  /**
   * Initialize a new Kegg2jSBML object, using the given cache.
   * @param manager
   */
  public KEGG2SBMLqual(KeggInfoManagement manager) {
    super(manager);
    
    loadPreferences();
  }
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
  }
  
  /**
   * 
   * @return the level and version of the SBML core (2,4)
   */
  protected ValuePair<Integer, Integer> getLevelAndVersion() {
    return new ValuePair<Integer, Integer>(Integer.valueOf(3),
        Integer.valueOf(1));
  }
  
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) {
    SBMLDocument doc = super.translateWithoutPreprocessing(p);
    // ... extend doc
    Model model = doc.getModel();
    qualModel = new QualitativeModel(model);
    doc.addDeclaredNamespace(KEGG2SBMLqual.QUAL_NS, KEGG2SBMLqual.QUAL_NS_PREFIX);
    model.addExtension(KEGG2SBMLqual.QUAL_NS, qualModel);
    
    // KEGG2jSBML always just creates one compartment
    Compartment compartment = model.getCompartment(0);
    
    // Give a warning if we have no relations.
    if (p.getRelations().size()<1) {
      log.warning("File does not contain any relations. Graph will look quite boring...");
    } else {
      for (Relation r : p.getRelations()) {
        addKGMLRelation(r, p, compartment);
      }  
    }
    
    return doc;
  }


  public Transition addKGMLRelation(Relation r, Pathway p, Compartment compartment) {
    // create transition and add it to the model
    Transition t = qualModel.createTransition(NameToSId("tr"));

    Species one = (Species) p.getEntryForId(r.getEntry1()).getCustom();
    Species two = (Species) p.getEntryForId(r.getEntry2()).getCustom();

    if (one==null || two==null) {
      System.out.println("Relation with unknown entry!");
      return null;
    }

    QualitativeSpecies qOne = createQualitativeSpeciesFromSpecies(one);
    QualitativeSpecies qTwo = createQualitativeSpeciesFromSpecies(two);
   
    // Input
    Input in = t.createInput(NameToSId("in"), qOne, InputTransitionEffect.none); //TODO: is this correct?
    in.setMetaId("meta_" + in.getId());

    // Output
    t.createOutput(NameToSId("out"), qTwo, OutputTransitionEffect.assignmentLevel); //TODO: is this correct?    
    
    //TODO: function term

    Sign sign = null;
    // Determine the sign variable
    List<SubType> subTypes = r.getSubtypes();
    if (subTypes != null && subTypes.size() > 0) {
      Collection<String> subTypeNames = r.getSubtypesNames();

      if (subTypeNames.contains(SubType.INHIBITION)) {
        in.setSBOTerm(169);
        sign = Sign.dual;
      } else if (subTypeNames.contains(SubType.ACTIVATION)) { //stimulation SBO:0000170
        sign = Sign.positive;
        in.setSBOTerm(170);
      } else if(subTypeNames.contains(SubType.REPRESSION)) { //inhibition, repression SBO:0000169
        sign = Sign.negative;
        in.setSBOTerm(169);
      } else if(subTypeNames.contains(SubType.INDIRECT_EFFECT)) { // indirect effect 
        sign = Sign.unknown;        
      } else if(subTypeNames.contains(SubType.STATE_CHANGE)) { //state change SBO:0000168        
        sign = Sign.unknown; //TODO: is unknown correct?
        in.setSBOTerm(168);
      } else {
        // just for debugging
        sign = Sign.unknown;
      }


      CVTerm cv= new CVTerm(CVTerm.Qualifier.BQB_IS);
      if (sign!=null){          
        if(subTypeNames.contains("dissociation")) { // dissociation SBO:0000177
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(177));
        } 
        if(subTypeNames.contains("binding/association")) { // binding/association SBO:0000177
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(177));   
        } 
        if (subTypeNames.contains("phosphorylation")) { // phosphorylation SBO:0000216
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(216));
        } 
        if (subTypeNames.contains("dephosphorylation")) { // dephosphorylation SBO:0000330
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(330));
        } 
        if (subTypeNames.contains("glycosylation")) { // glycosylation SBO:0000217
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(217));
        } 
        if (subTypeNames.contains("ubiquitination")) { // ubiquitination SBO:0000224
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(224));
        } 
        if (subTypeNames.contains("methylation")) { // methylation SBO:0000214
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(214));
        }
        
        if (cv.getNumResources()>0) {          
          setBiologicalQualifierISorHAS_VERSION(cv);
          in.addCVTerm(cv);
        }
        
        in.setSign(sign);
      }
    }

    return t;

  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO%3A0000177".
   * @param i
   * @return
   */
  private String formatSBO(int i) {
    StringBuilder b = new StringBuilder("SBO%3A");
    String iString = Integer.toString(i);
    b.append(Utils.replicateCharacter('0', 7-iString.length()));
    b.append(iString);
    return b.toString();
  }

  private QualitativeSpecies createQualitativeSpeciesFromSpecies(Species species) {
    String id = "qual_" + species.getId();
    QualitativeSpecies qs = qualModel.getQualitativeSpecies(id);
    if(qs == null){
      qs = qualModel.createQualitativeSpecies(id, species.getBoundaryCondition(), species.getCompartment(), species.getConstant());
      qs.setName(species.getName());
      qs.setSBOTerm(species.getSBOTerm()); 
      qs.setMetaId("meta_" + id);
    }
    
    return qs;  
  }
  
  /**
   * Provides some direct access to KEGG2JSBML functionalities.
   * @param args
   * @throws Exception 
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XMLStreamException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws Exception {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    Format format = Format.SBML_QUAL;
    AbstractKEGGtranslator<SBMLDocument> k2s;
    KeggInfoManagement manager = null;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
    }
    k2s = (AbstractKEGGtranslator<SBMLDocument>) BatchKEGGtranslator.getTranslator(format, manager);
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1)
          batch.setChangeOutdirTo(args[1]);
        batch.setTranslator(k2s);
        batch.setOutFormat(format);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) outfile = args[1];
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        k2s.translate(p, outfile);
      }
      
      // Remember already queried objects (save cache)
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
      return;
    }
    
    
    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
//      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      SBMLDocument doc = k2s.translate(new File("files/KGMLsamplefiles/hsa04210.xml"));
      new SBMLWriter().write(doc, "files/KGMLsamplefiles/hsa04210.sbml.xml");
      new JSBMLvisualizer(doc); 
            
      // Remember already queried objects
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }
}
