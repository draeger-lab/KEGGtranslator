package de.zbit.kegg.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.test.gui.JSBMLvisualizer;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.KeggInfoManagement;
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
    //doc.addDeclaredNamespace(KEGG2SBMLqual.QUAL_NS, KEGG2SBMLqual.QUAL_NS_PREFIX);
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
    Input in = t.createInput(NameToSId("in"), qOne, InputTransitionEffect.none);

    // Output
    t.createOutput(NameToSId("out"), qTwo, OutputTransitionEffect.assignmentLevel); //TODO: is this correct?    
    
    //TODO: function term?

    
    
    Sign sign = null;
//    // Determine the sign variable
//    if (r.getSubtypes()!=null && r.getSubtypes().size()>0) {
//      ArrayList<String> subTypeNames = r.getSubtypesNames();
//
//      if (subTypeNames.contains("-->") && subTypeNames.contains("--|")) { //inhibition, repression SBO:0000169
//        sign = Sign.dual;
//      } else if (subTypeNames.contains("-->")) { //activation SBO:0000170
//        sign = Sign.positive;
//      } else if(subTypeNames.contains("--|")) { //inhibition, repression SBO:0000169
//        sign = Sign.negative;
//      } else if(subTypeNames.contains("..>")) { // indirect effect 
//        sign = Sign.unknown;
//      } else if(subTypeNames.contains("...")) { //state change SBO:0000168        
//        sign = Sign.unknown; //TODO: is unknown correct?
//      } 
//
//
//      if (sign!=null){          
//        if(subTypeNames.contains("-+-")) { // dissociation SBO:0000177
//          in.setSBOTerm("SBO:0000177");  
//        } else if(subTypeNames.contains("---")) { // binding/association SBO:0000177
//          in.setSBOTerm("SBO:0000177");   
//        } else if (subTypeNames.contains("+p")) { // phophorylation SBO:0000216
//          in.setSBOTerm("SBO:0000216");
//        } else if (subTypeNames.contains("-p")) { // dephosphorylation SBO:0000330
//          in.setSBOTerm("SBO:0000330");
//        } else if (subTypeNames.contains("+g")) { // glycosylation SBO:0000217
//          in.setSBOTerm("SBO:0000217");
//        } else if (subTypeNames.contains("+u")) { // ubiquitination SBO:0000224
//          in.setSBOTerm("SBO:0000224");
//        } else if (subTypeNames.contains("+m")) { // methylation SBO:0000214
//          in.setSBOTerm("SBO:0000214");
//        }
//        
//        in.setSign(sign);
//      }
    
    
    // Determine the sign variable
    List<SubType> subTypes = r.getSubtypes();
    if (subTypes != null && subTypes.size() > 0) {
      List<String> subTypeNames = r.getSubtypesNames();

      if (subTypeNames.contains(SubType.INHIBITION)) {
          in.setSBOTerm("SBO:0000169");
          sign = Sign.dual;
      } else if (subTypeNames.contains(SubType.ACTIVATION)) { //stimulation SBO:0000170
        sign = Sign.positive;
        in.setSBOTerm("SBO:0000170");
      } else if(subTypeNames.contains(SubType.REPRESSION)) { //inhibition, repression SBO:0000169
        sign = Sign.negative;
        in.setSBOTerm("SBO:0000169");
      } else if(subTypeNames.contains(SubType.INDIRECT_EFFECT)) { // indirect effect 
        sign = Sign.unknown;
        
      } else if(subTypeNames.contains(SubType.STATE_CHANGE)) { //state change SBO:0000168        
        sign = Sign.unknown; //TODO: is unknown correct?
        in.setSBOTerm("SBO:0000168");
      } else {
        // just for debugging
        sign = null;
      }


      if (sign!=null){          
        // TODO: put this in an annotation
//        if(subTypeNames.contains("dissociation")) { // dissociation SBO:0000177
//          in.setSBOTerm("SBO:0000177");  
//        } else if(subTypeNames.contains("binding/association")) { // binding/association SBO:0000177
//          in.setSBOTerm("SBO:0000177");   
//        } else if (subTypeNames.contains("phosphorylation")) { // phosphorylation SBO:0000216
//          in.setSBOTerm("SBO:0000216");
//        } else if (subTypeNames.contains("dephosphorylation")) { // dephosphorylation SBO:0000330
//          in.setSBOTerm("SBO:0000330");
//        } else if (subTypeNames.contains("glycosylation")) { // glycosylation SBO:0000217
//          in.setSBOTerm("SBO:0000217");
//        } else if (subTypeNames.contains("ubiquitination")) { // ubiquitination SBO:0000224
//          in.setSBOTerm("SBO:0000224");
//        } else if (subTypeNames.contains("methylation")) { // methylation SBO:0000214
//          in.setSBOTerm("SBO:0000214");
//        }
        
        in.setSign(sign);
      }
    }

    return t;

  }
  
  private QualitativeSpecies createQualitativeSpeciesFromSpecies(Species species) {
    String id = "qual_" + species.getId();
    QualitativeSpecies qs = qualModel.getQualitativeSpeciesWithID(id);
    if(qs != null){
      return qs;
    } else {
      QualitativeSpecies qSpecies = qualModel.createQualitativeSpecies(id, species.getBoundaryCondition(), species.getCompartment(), species.getConstant());
      qSpecies.setName(species.getName());
      qSpecies.setSBOTerm(species.getSBOTerm()); //TODO: not possible, because, the level is wrong!
      
      return qSpecies;  
    }
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
      
      SBMLDocument doc = k2s.translate(new File("files/KGMLsamplefiles/hsa05212.xml"));
      new SBMLWriter().write(doc, "files/KGMLsamplefiles/hsa05212.sbml.xml");
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