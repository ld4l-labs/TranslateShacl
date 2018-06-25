import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;


public class TranslateShacl {
	public static int configNumber = 200;
	public static Model ontologyModel = null;

	
	public static void main(String[] args) {
		//For better option
		//Run translation
		//HipHopTranslation ht = new HipHopTranslation();
		//ht.doTranslate(args);
		
		//ARMTranslation art = new ARMTranslation();
		//art.doTranslate(args);
		
		//cleanupLanguageTags();
		
		ArtFrameTranslation artframe = new ArtFrameTranslation();
		artframe.doTranslate(args);
		
		
	}
	
	//Certain vocabulary rdf files needed language tags filtered out
	private static void cleanupLanguageTags() {
		//Temporarily adding for extracting statements that have a language with a dash
		
		//Read in from a specific directory instead of just a file
		List<String> files = new ArrayList<String>();
		files.add("aat_binding_component.rdf");
		files.add("aat_handwriting_type.rdf");
		files.add("aat_style_period.rdf");
		files.add("aat_typeface.rdf");
		
		for(String f: files) {
			Model model= ModelFactory.createDefaultModel();
			Model removeModel = ModelFactory.createDefaultModel();
			File fileEntry = new File("rdf/ARMOntology/sources/vocabularies/" + f);
			System.out.println("*****File name*******=" + f);
			try {
			
				FileInputStream fis = (new FileInputStream(fileEntry));
				model.read( fis, null, "RDF/XML" );
				String query = "SELECT ?subject ?predicate ?literal (lang(?literal) AS ?lang) WHERE {" + 
				"?subject ?predicate ?literal . FILTER(strlen(lang(?literal)) > 10)" + 
				"}";
				Query q= QueryFactory.create(query);
				ResultSet rs = null;
				QueryExecution qe = QueryExecutionFactory.create(q, model);
				
				try {
					rs = qe.execSelect();
					while(rs.hasNext()) {
						QuerySolution qs = rs.next();
						System.out.println(qs.toString());
						removeModel.addLiteral(qs.getResource("subject"), ResourceFactory.createProperty(qs.getResource("predicate").getURI()), qs.getLiteral("literal"));
					}
					Model resultingModel = model.remove(removeModel);
					System.out.println("Output new model: " + f);
					resultingModel.write(System.out, "RDF/XML");
					
				} catch(Exception ex) {
					System.out.println("Error executing this query");
				}
				
			} catch(Exception ex) {
				System.out.println("Error occurred in reading in SHACL files: " + fileEntry.getName());
				ex.printStackTrace();
			}
		    
		}
	}
	
	
	/***************** HipHop translation ******************/
	public static class HipHopTranslation extends Translation {
		public HipHopTranslation() {
			this.shaclDirectoryPath = "rdf/SHACLFiles";
			this.ontologyDirectoryPath = "rdf/currentOntologyFiles";
			this.workFormURI = "http://bibliotek-o.org/shapes/audio/AudioWorkForm";
			this.instanceFormURI = "http://bibliotek-o.org/shapes/audio/AudioInstanceForm";
			this.itemFormURI = "http://bibliotek-o.org/shapes/audio/AudioItemForm";
		}
		
		protected  void generateCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			String configURI = retrieveConfigURI("http://purl.org/dc/terms/hasPart", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Audio", appModel);
			rdfString += generateConfigRDF(configURI, "hasPart", "http://id.loc.gov/ontologies/bibframe/Audio") + "\n";
			
			//Genre form, audio to concept
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Audio", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
			//has activity - audio work, instance, item
			configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Audio", "http://bibliotek-o.org/ontology/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "hasActivity", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
			//Subject
			//http://www.w3.org/2002/07/owl#
			configURI =  retrieveConfigURI("http://purl.org/dc/terms/subject", "http://id.loc.gov/ontologies/bibframe/Audio", "http://www.w3.org/2002/07/owl#Thing", appModel);
			rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
					
			//hasPreferredTitle
			configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
			
			//Title
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/title", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "title", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
			
			//Note
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/note", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Note", appModel);
			rdfString += generateConfigRDF(configURI, "note", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
			
			//event (recorded at)
			configURI =  retrieveConfigURI("http://schema.org/recordedAt", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Event", appModel);
			rdfString += generateConfigRDF(configURI, "event", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
				
			//Identifier
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/identifiedBy", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Identifier", appModel);
			rdfString += generateConfigRDF(configURI, "identifier", "http://id.loc.gov/ontologies/bibframe/Audio")+ "\n";
		
			
			System.out.println(rdfString);
		}
		
		protected  void generateInstanceCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			String configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Instance", "http://bibliotek-o.org/ontology/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "hasActivity", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
		
			//has preferred title
			configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Instance", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//title
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/title", "http://id.loc.gov/ontologies/bibframe/Instance", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "title", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//note
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/note", "http://id.loc.gov/ontologies/bibframe/Instance", "http://id.loc.gov/ontologies/bibframe/Note", appModel);
			rdfString += generateConfigRDF(configURI, "note", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			
			//identifier
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/identifiedBy", "http://id.loc.gov/ontologies/bibframe/Instance", "http://id.loc.gov/ontologies/bibframe/Identifier", appModel);
			rdfString += generateConfigRDF(configURI, "identifier", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//measurement group
			configURI =  retrieveConfigURI("http://measurement.bibliotek-o.org/hasMeasurementGroup", "http://id.loc.gov/ontologies/bibframe/Instance", "http://measurement.bibliotek-o.org/MeasurementGroup", appModel);
			rdfString += generateConfigRDF(configURI, "measurementGroup", "http://measurement.bibliotek-o.org/MeasurementGroup")+ "\n";
				
			
			System.out.println(rdfString);
		}
		
		protected void generateItemCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			String configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Item", "http://bibliotek-o.org/ontology/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "hasActivity", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
		
			
			//preferred title
			configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Item", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "hasPreferredTitle", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
		
			
			//title
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/title", "http://id.loc.gov/ontologies/bibframe/Item", "http://id.loc.gov/ontologies/bibframe/Title", appModel);
			rdfString += generateConfigRDF(configURI, "title", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			System.out.println(rdfString);
			
			//note
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/note", "http://id.loc.gov/ontologies/bibframe/Item", "http://id.loc.gov/ontologies/bibframe/Note", appModel);
			rdfString += generateConfigRDF(configURI, "note", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			System.out.println(rdfString);
			
			//identifier
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/identifiedBy", "http://id.loc.gov/ontologies/bibframe/Item", "http://id.loc.gov/ontologies/bibframe/Identifier", appModel);
			rdfString += generateConfigRDF(configURI, "identifier", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
		
		}
		
		protected String generateConfigRDF(String configURI, String property, String domainURI) {
			String configRDF = "";
			switch (property) {
				case "hasPart":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-workHasPartWork.xml\"^^xsd:string ." + 
							"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
								 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"workHasPartWork.jsonld\" .";
	
					break;
				case "genreForm":
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"hasGenreForm.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"hasGenreForm.ftl\" .";
	
					break;	
				case "hasActivity":
					configRDF =  "<" + configURI + "> :listViewConfigFile \"listViewConfig-workHasActivity.xml\"^^xsd:string ; " + 
							" <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string>  ;";
	
					//Depends on whether audio, instance or item
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Audio")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioInstanceHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioItemHasActivity.jsonld\" ; ";
					}	  	  
					
					configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"hasActivity.ftl\" .";
					//"<" + configURI + "> :listViewConfigFile \"listViewConfig-instanceHasActivity.xml\"^^xsd:string .  " + 
							 
	
					break;	
				case "subject":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-subject.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"hasLCSH.jsonld\"; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"hasLCSH.ftl\" .";
	
					break;
				case "hasPreferredTitle":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-hasPreferredTitle.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " +
							"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkPreferredTitle.jsonld\". ";				
	
					//In case this depends on whether audio, instance, or item, right now attaching same one for all classes
					//Depends on whether audio, instance or item
					/*
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Audio")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkPreferredTitle.jsonld\". ";				} 
					else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioInstancePreferredTitle.jsonld\". ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioItemPreferredTitle.jsonld\". " ;
					}*/	  	  
					
	
					break;	
				case "title":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-title.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " ; 
					configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkTitle.jsonld\". " ;
			
					//This will be simpler for now, just putting in one version
					
					/*
					//Depends on whether audio, instance or item
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Audio")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkTitle.jsonld\"; " + 
								 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"audioWorkTitle.ftl\" .";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioInstanceTitle.jsonld\"; " + 
								 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"audioInstanceTitle.ftl\" .";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioItemTitle.jsonld\"; " + 
								 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"audioItemTitle.ftl\" .";
					}	else {
						configRDF += " .";
					}
					
					*/
					break;
				case "note":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-note.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " ; 
							
					
					
					//Depends on whether audio, instance or item
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Audio")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioWorkNote.jsonld\" . " ;
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioInstanceNote.jsonld\". " ;
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"audioItemNote.jsonld\". " ;
					}	else {
						configRDF += " .";
					}
					
	
					break;	
			
				case "event":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-event.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
					"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"recordedAt.ftl\" ; ";				
					configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"recordedAt.jsonld\" ;" + 
								"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customModelChangePreprocessorAnnot> \"\"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors.AuthorityHackPreprocessor\"^^<http://www.w3.org/2001/XMLSchema#string> ." ;
	
					break;
					
				case "identifier": 
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"hasIdentifier.jsonld\" .";
					break;
					
				case "measurementGroup":
					//configRDF = "<" + configURI + "> 
					
				//all sound characteristics require custom configuration for list views
				case "soundCharacteristic":
					break;
					
				default:
					break;
					
					
			}
			return configRDF;
		}
		
		
	}
	
	/*************** ARM Translation *********************/
	public static class ARMTranslation extends Translation {
		public ARMTranslation() {
			this.shaclDirectoryPath = "rdf/ARMSHACL";
			this.ontologyDirectoryPath = "rdf/ARMOntology";
			//https://w3id.org/arm/application_profile/shacl/raremat_monograph/
			//https://w3id.org/arm/application_profiles/raremat_monograph/shacl/raremat_monograph_form/
			String namespace = "https://w3id.org/arm/application_profiles/raremat_monograph/shacl/raremat_monograph_form/";
			this.workFormURI = namespace + "WorkForm";
			this.instanceFormURI = namespace + "InstanceForm";
			this.itemFormURI = namespace + "ItemForm";
		}
		
		protected  void generateCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			//String configURI = retrieveConfigURI("http://purl.org/dc/terms/hasPart", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Audio", appModel);
			//rdfString += generateConfigRDF(configURI, "hasPart", "http://id.loc.gov/ontologies/bibframe/Audio") + "\n";
			
			//activity
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Text", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Text", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			//Subject heading
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Text", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			
			System.out.println(rdfString);
		}
		
		protected  void generateInstanceCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasCitation", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/ontology/0.1/Citation", appModel);
			rdfString += generateConfigRDF(configURI, "hasCitation", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//marking
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/markedBy", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2002/07/owl#Thing", appModel);
			rdfString += generateConfigRDF(configURI, "markedBy", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//binding
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/hasPart", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/ontology/0.1/Binding", appModel);
			rdfString += generateConfigRDF(configURI, "binding", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//activity
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//Subject heading - doesn't appear to be applicable at instance level
			//configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			//rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			//Measurement Group
			//https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup
			configURI =  retrieveConfigURI("https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/measurement/ontology/0.1/MeasurementGroup", appModel);
			rdfString += generateConfigRDF(configURI, "measurement", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			System.out.println(rdfString);
		}
		
		protected void generateItemCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			//String configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Item", "http://bibliotek-o.org/ontology/Activity", appModel);
			//rdfString += generateConfigRDF(configURI, "hasActivity", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasCitation", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/Citation", appModel);
			rdfString += generateConfigRDF(configURI, "hasCitation", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
		
			//marking
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/markedBy", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2002/07/owl#Thing", appModel);
			rdfString += generateConfigRDF(configURI, "markedBy", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			
			//binding
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/hasPart", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/Binding", appModel);
			rdfString += generateConfigRDF(configURI, "binding", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//physical condition
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasPhysicalCondition", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/PhysicalCondition", appModel);
			rdfString += generateConfigRDF(configURI, "hasPhysicalCondition", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//extent
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/extent", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/PaginationFoliation", appModel);
			rdfString += generateConfigRDF(configURI, "extent", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Title
			
			//Preferred Title
			
			//Activity
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			
			//Measurement Group
			configURI =  retrieveConfigURI("https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/measurement/ontology/0.1/MeasurementGroup", appModel);
			rdfString += generateConfigRDF(configURI, "measurement", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Subject heading - doesn't appear to be relevant at subject level
			//configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			//rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

		
			
			System.out.println(rdfString);

		}
		
		protected String generateConfigRDF(String configURI, String property, String domainURI) {
			String configRDF = "";
			switch (property) {
			
			case "hasCitation":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasCitation.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasCitation.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasCitationForm.ftl\".";

				break;
			case "markedBy":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-markedBy.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armMarkedBy.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armMarkedByForm.ftl\".";

				break;
			case "binding":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasPartEnclosureBinding.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPartBinding.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasPartBindingForm.ftl\".";

				break;
			case "hasPhysicalCondition":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-itemHasPhysicalCondition.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPhysicalCondition.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasPhysicalConditionForm.ftl\".";

				break;
			case "extent":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-extent.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armExtent.jsonld\" . " ; 
							//extent = using custom 
							// "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasCitationForm.ftl\".";

				break;
				
		  /**Copying things over **/
				
				case "activity":
					configRDF =  "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasActivity.xml\"^^xsd:string ; " + 
							" <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string>  ;";
	
					//Depends on whether audio, instance or item
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Text")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armTextHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armInstanceHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armItemHasActivity.jsonld\" ; ";
					}	  	  
					
					configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasActivity.ftl\" .";
					//"<" + configURI + "> :listViewConfigFile \"listViewConfig-instanceHasActivity.xml\"^^xsd:string .  " + 
							 
	
					break;	
				case "measurement":
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasMeasurementGroup.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasMeasurement.ftl\" .";
	
					break;		
				case "genreForm":
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasGenreForm.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasGenreForm.ftl\" .";
	
					break;	
				case "subject":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-subject.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasLCSH.jsonld\"; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasLCSH.ftl\" .";
	
					break;
				case "hasPreferredTitle":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-hasPreferredTitle.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " +
							"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPreferredTitle.jsonld\". ";				
	
					
					break;	
				case "title":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-title.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " ; 
					configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armTitle.jsonld\". " ;
				
			
				default:
					break;
					
					
			}
			return configRDF;
		}
		
	}
	
	
	/*************** ArtFrame Translation *********************/
	public static class ArtFrameTranslation extends Translation {
		public ArtFrameTranslation() {
			this.shaclDirectoryPath = "rdf/ArtFrameSHACL";
			this.ontologyDirectoryPath = "rdf/ArtFrameOntology";
			String namespace = "https://w3id.org/arm/application_profiles/art/shacl/artframe_art_form/";
			this.workFormURI = namespace + "WorkForm";
			this.instanceFormURI = namespace + "InstanceForm";
			this.itemFormURI = namespace + "ItemForm";
		}
		
		protected  void generateCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			//String configURI = retrieveConfigURI("http://purl.org/dc/terms/hasPart", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Audio", appModel);
			//rdfString += generateConfigRDF(configURI, "hasPart", "http://id.loc.gov/ontologies/bibframe/Audio") + "\n";
			
			//activity
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Text", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Text", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			//Subject heading
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Text", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Text")+ "\n";

			
			System.out.println(rdfString);
		}
		
		protected  void generateInstanceCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasCitation", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/ontology/0.1/Citation", appModel);
			rdfString += generateConfigRDF(configURI, "hasCitation", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//marking
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/markedBy", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2002/07/owl#Thing", appModel);
			rdfString += generateConfigRDF(configURI, "markedBy", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//binding
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/hasPart", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/ontology/0.1/Binding", appModel);
			rdfString += generateConfigRDF(configURI, "binding", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//activity
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";
			
			//Subject heading - doesn't appear to be applicable at instance level
			//configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Instance", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			//rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			//Measurement Group
			//https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup
			configURI =  retrieveConfigURI("https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup", "http://id.loc.gov/ontologies/bibframe/Instance", "https://w3id.org/arm/measurement/ontology/0.1/MeasurementGroup", appModel);
			rdfString += generateConfigRDF(configURI, "measurement", "http://id.loc.gov/ontologies/bibframe/Instance")+ "\n";

			System.out.println(rdfString);
		}
		
		protected void generateItemCustomFormSpecifics(Model appModel) {
			//App Model uses the vitroLib-specific faux configuration
			//TODO: Make this use classes instead
			//Base URI -> <domain =, range= >
			String rdfString = "";
			//has part custom form
			//String configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Item", "http://bibliotek-o.org/ontology/Activity", appModel);
			//rdfString += generateConfigRDF(configURI, "hasActivity", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			String configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasCitation", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/Citation", appModel);
			rdfString += generateConfigRDF(configURI, "hasCitation", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
		
			//marking
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/markedBy", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2002/07/owl#Thing", appModel);
			rdfString += generateConfigRDF(configURI, "markedBy", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";
			
			//binding
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/hasPart", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/Binding", appModel);
			rdfString += generateConfigRDF(configURI, "binding", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//physical condition
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/ontology/0.1/hasPhysicalCondition", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/PhysicalCondition", appModel);
			rdfString += generateConfigRDF(configURI, "hasPhysicalCondition", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//extent
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/extent", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/ontology/0.1/PaginationFoliation", appModel);
			rdfString += generateConfigRDF(configURI, "extent", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Genre Form
			configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
			rdfString += generateConfigRDF(configURI, "genreForm", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Title
			
			//Preferred Title
			
			//Activity
			configURI =  retrieveConfigURI("https://w3id.org/arm/core/activity/0.1/hasActivity", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/core/activity/0.1/Activity", appModel);
			rdfString += generateConfigRDF(configURI, "activity", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			
			//Measurement Group
			configURI =  retrieveConfigURI("https://w3id.org/arm/measurement/ontology/0.1/hasMeasurementGroup", "http://id.loc.gov/ontologies/bibframe/Item", "https://w3id.org/arm/measurement/ontology/0.1/MeasurementGroup", appModel);
			rdfString += generateConfigRDF(configURI, "measurement", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

			//Subject heading - doesn't appear to be relevant at subject level
			//configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/subject", "http://id.loc.gov/ontologies/bibframe/Item", "http://www.w3.org/2000/01/rdf-schema#Resource", appModel);
			//rdfString += generateConfigRDF(configURI, "subject", "http://id.loc.gov/ontologies/bibframe/Item")+ "\n";

		
			
			System.out.println(rdfString);

		}
		
		protected String generateConfigRDF(String configURI, String property, String domainURI) {
			String configRDF = "";
			switch (property) {
			
			case "hasCitation":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasCitation.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasCitation.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasCitationForm.ftl\".";

				break;
			case "markedBy":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-markedBy.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armMarkedBy.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armMarkedByForm.ftl\".";

				break;
			case "binding":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasPartEnclosureBinding.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPartBinding.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasPartBindingForm.ftl\".";

				break;
			case "hasPhysicalCondition":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-itemHasPhysicalCondition.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPhysicalCondition.jsonld\" ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasPhysicalConditionForm.ftl\".";

				break;
			case "extent":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-extent.xml\"^^xsd:string ." + 
						"<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armExtent.jsonld\" . " ; 
							//extent = using custom 
							// "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasCitationForm.ftl\".";

				break;
				
		  /**Copying things over **/
				
				case "activity":
					configRDF =  "<" + configURI + "> :listViewConfigFile \"listViewConfig-ARM-hasActivity.xml\"^^xsd:string ; " + 
							" <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string>  ;";
	
					//Depends on whether audio, instance or item
					if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Text")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armTextHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Instance")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armInstanceHasActivity.jsonld\" ; ";
					} else if(domainURI.equals("http://id.loc.gov/ontologies/bibframe/Item")) {
						configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armItemHasActivity.jsonld\" ; ";
					}	  	  
					
					configRDF += "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasActivity.ftl\" .";
					//"<" + configURI + "> :listViewConfigFile \"listViewConfig-instanceHasActivity.xml\"^^xsd:string .  " + 
							 
	
					break;	
				case "measurement":
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasMeasurementGroup.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasMeasurement.ftl\" .";
	
					break;		
				case "genreForm":
					configRDF = "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasGenreForm.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasGenreForm.ftl\" .";
	
					break;	
				case "subject":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-subject.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasLCSH.jsonld\"; " + 
							 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"armHasLCSH.ftl\" .";
	
					break;
				case "hasPreferredTitle":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-hasPreferredTitle.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " +
							"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armHasPreferredTitle.jsonld\". ";				
	
					
					break;	
				case "title":
					configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-arm-title.xml\"^^xsd:string .  ";
					configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " ; 
					configRDF +=  "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"armTitle.jsonld\". " ;
				
			
				default:
					break;
					
					
			}
			return configRDF;
		}

	}
	
	/******** Abstract class Translation, with partial implementations of most methods *********/
	
	public static abstract class Translation {
		//Directory paths are set based on specific implementation
		protected static String ontologyDirectoryPath = null;
		protected static String shaclDirectoryPath = null;
		protected static String workFormURI = null;
		protected static String instanceFormURI = null;
		protected static String itemFormURI = null;

		public static String getWorkFormURI() {
			return workFormURI;
		}

		public static String getInstanceFormURI() {
			return instanceFormURI;
		}

		public static String getItemFormURI() {
			return itemFormURI;
		}

		
		public static String getOntologyDirectoryPath() {
			return ontologyDirectoryPath;
		}
		
		public static String getShaclDirectoryPath() {
			return shaclDirectoryPath;
		}
	

		
		
		public void doTranslate (String[] args) {
			
			Model shaclModel = populateModel();
			//Populate this for reference later
			ontologyModel = getOntologyModel();
			compareToOntology(shaclModel, ontologyModel);
			//Output any RDF property (that is not defined as an owl object property)
			System.out.println("Check for properties identified as RDF property, classes identified as RDFS class");
			outputRDFProperties(ontologyModel);
			outputRDFClasses(ontologyModel);
			//Do data property specifics
			System.out.println("********Generate data properties*********");
			Model dataPropertiesModel = processDataProperties(shaclModel, ontologyModel);
			//Write out data properties model
			dataPropertiesModel.write(System.out, "N3");
			 
			//Generate property groups
			System.out.println("********Generate Property Groups***********");
			generatePropertyGroups(shaclModel);
			System.out.println("********Generate Work*********");
			//Work level info		
			Model workAppModel = processWork(shaclModel);
			System.out.println("********Generate Instance*********");
			//Instance level info
			Model instanceAppModel = processInstance(shaclModel);
			System.out.println("********Generate Item*********");
	
			//Item level info
			Model itemAppModel = processItem(shaclModel);
			
			
			//Generate custom form specifics
			System.out.println("#Work custom form");
			generateCustomFormSpecifics(workAppModel);
			System.out.println("#Instance custom form");
			generateInstanceCustomFormSpecifics(instanceAppModel);
			System.out.println("#Item custom form");
			generateItemCustomFormSpecifics(itemAppModel);
			//Generate dropdowns
			System.out.println("Custom form dropdowns");
			generateCustomFormDropdowns(shaclModel, ontologyModel);
	
		}
		
		private void outputRDFProperties(Model ontologyModel) {
			List<String> outputList = new ArrayList<String>();
			String query = "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
		 		"PREFIX owl:   <http://www.w3.org/2002/07/owl#> " + 
		 		"PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> " + 
		 		"SELECT DISTINCT ?propertyURI ?range WHERE {?propertyURI rdf:type rdf:Property . " + 
		 		" OPTIONAL {?propertyURI rdfs:range ?range . } } ";
			ResultSet rs = executeQuery(ontologyModel, query);
			while(rs.hasNext()) {
				String type = "object";
				QuerySolution qs = rs.nextSolution();
				Resource propertyURI = qs.getResource("propertyURI");
				if(qs.contains("range")) {
					Resource range = qs.getResource("range");
					if(range != null && range.getURI().equals(RDFS.Literal.getURI())) {
						type = "data";
					}
				}
				String output = "<" + propertyURI + "> <" + RDF.type.getURI() + "> <";
				if(type.equals("object")) {
					output += OWL.ObjectProperty.getURI();
				} else {
					output += OWL.DatatypeProperty.getURI();
				}
				output += "> .";
				outputList.add(output);
			}
			Collections.sort(outputList);
			for(String o: outputList) {
				System.out.println(o);
			}
			
		}
		
		//code to find RDF Classes and spit out statements specifying them as OWL Classes
		private void outputRDFClasses(Model ontologyModel) {
			List<String> outputList = new ArrayList<String>();
			String query = "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
		 		"PREFIX owl:   <http://www.w3.org/2002/07/owl#> " + 
		 		"PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> " + 
		 		"SELECT DISTINCT ?classURI WHERE {?classURI rdf:type rdfs:Class .  } ";
			ResultSet rs = executeQuery(ontologyModel, query);
			while(rs.hasNext()) {
				
				QuerySolution qs = rs.nextSolution();
				Resource classURI = qs.getResource("classURI");
			
				String output = "<" + classURI + "> <" + RDF.type.getURI() + "> <" + OWL.Class.getURI() +  "> .";
				outputList.add(output);
			}
			
			Collections.sort(outputList);
			for(String o: outputList) {
				System.out.println(o);
			}
			
		}

		//Generate data properties
		 private static Model processDataProperties(Model shaclModel, Model ontologyModel){
			 Model dataPropertiesModel = ModelFactory.createDefaultModel();
			 //Query for all properties 
			 String query = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
			 "SELECT DISTINCT ?formURI ?propertyPathURI ?path ?formTarget ?propertyTarget ?group WHERE {" + 
			 "?formURI sh:property ?propertyPathURI . " + 
			 "?propertyPathURI sh:nodeKind sh:Literal ."  +
			 "?propertyPathURI sh:path ?path . " +
			 "OPTIONAL {?property sh:group ?group .} " + 
			 "OPTIONAL {?propertyPathURI sh:target ?propertyTarget . } " + 
			 "OPTIONAL {?formURI sh:targetClass ?formTarget. }" + 
			 "}";
			 ResultSet rs = executeQuery(shaclModel, query);
		
			 while(rs.hasNext()) {
				 QuerySolution qs = rs.nextSolution();
				 Model propertyModel = createDataProperty(qs);
				 if(!containsDataRestriction(dataPropertiesModel, propertyModel)) {
					 dataPropertiesModel.add(propertyModel);
				 }
			 }
			 
			 return dataPropertiesModel;
		 }
		
		//Data properties model = aggregate model of propertyModel, propertyModel = single data property restriction
		 private static boolean containsDataRestriction(Model dataPropertiesModel, Model propertyModel) {
			//Check if restriction for data property exists already, don't add again
			 String prefixes = "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
			 		"PREFIX owl:   <http://www.w3.org/2002/07/owl#> " + 
			 		"PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ";
			String query = prefixes + "SELECT  ?domain ?property ?valuesFrom WHERE {?domain rdfs:subClassOf ?restriction ." + 
			"?restriction owl:allValuesFrom ?valuesFrom . ?restriction owl:onProperty ?property .}";
			ResultSet rs = executeQuery(propertyModel, query);
			while(rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				Resource domain = qs.getResource("domain");
				Resource property = qs.getResource("property");
				Resource valuesFrom = qs.getResource("valuesFrom");
				
				String checkQuery = prefixes + "SELECT  ?restriction WHERE { <" + domain.getURI() +  "> rdfs:subClassOf ?restriction ." + 
						"?restriction owl:allValuesFrom <" + valuesFrom.getURI() + "> . ?restriction owl:onProperty <" + property.getURI() + ">}";
				ResultSet checkRs = executeQuery(dataPropertiesModel, checkQuery);
				if(checkRs.hasNext()) {
					return true;
				}
			}
			
			return false;
		}

		private static Model createDataProperty(QuerySolution qs) {
				Model dataPropertyModel = ModelFactory.createDefaultModel();
				Resource path = getVarResource(qs, "path");
				String prefixes = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n" + 
						"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" + 
						"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .";
				String dataPropertyRDF = "";
				String propertyGroupURI = "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#inPropertyGroupAnnot>";
				if(path != null) {
					String pathURI = path.getURI();
					if(StringUtils.isEmpty(pathURI)) {
						System.out.println("This is an odd animal, not using since null or starts with file:" + qs.toString());
					} else {
						String pathResource = "<" + pathURI + ">";
				
						Resource group = getVarResource(qs, "group");
						Resource target = getVarResource(qs, "propertyTarget");
						Resource formTarget = getVarResource(qs, "formTarget");
						
						//Restriction on domain, picking string for now but
						//can also check for datatype but we are not currently
						if(group != null) {
							dataPropertyRDF = pathResource + " " + propertyGroupURI + " <" + group.getURI() + "> .";
						}
						//check target specified on the property first
						//if that is not available, check shapetarget
						String domainURI = "";
						if(target != null) {
							domainURI = "<" + target.getURI() + ">";
						} else {
							if(formTarget != null) {
								domainURI = "<" + formTarget.getURI() + ">";
							}
						}
						
						if(StringUtils.isNotEmpty(domainURI)) {
							String restrictionRDF = domainURI + " rdfs:subClassOf " + 
						" [ a owl:Restriction; " + 
						" owl:onProperty " + pathResource + ";" + 
						"owl:allValuesFrom <http://www.w3.org/2001/XMLSchema#string> ] .";
							dataPropertyRDF += restrictionRDF;
						}
					
					}
				
				}
				
				//Read in dataPropertyRDF with prefixes into model
				dataPropertyRDF = prefixes + dataPropertyRDF;
				dataPropertyModel.read(new ByteArrayInputStream(dataPropertyRDF.getBytes()), null, "N3");
				return dataPropertyModel;
			}

		//For each of work, instance, item models, generate the appropriate custom form linkages
		//Cases are hardcoded, but specific faux property config numbers change based on the original shacl file
		/*
		 
	 ## Not in SHACL yet so copied from original generated properties
	 # Annotation at work level, ties to specific jsonld
	 # Keep this as is because annotation not currently in SHACL but this will probably change
	 # Copied from original file here
	 local:fpgn11 a  :ConfigContext ; :configContextFor <http://bibliotek-o.org/ontology/isTargetOf>;:hasConfiguration local:fpgenconfig11 ; :qualifiedBy oa:Annotation ; :qualifiedByDomain bf:Work .	local:fpgenconfig11 a  :ObjectPropertyDisplayConfig ; vitro:collateBySubclassAnnot   false ; vitro:displayLimitAnnot "0"^^xsd:int ; vitro:displayRankAnnot  "0"^^xsd:int ; vitro:hiddenFromDisplayBelowRoleLevelAnnot role:public ;	vitro:hiddenFromPublishBelowRoleLevelAnnot role:public ; vitro:offerCreateNewOptionAnnot true ; vitro:prohibitedFromUpdateBelowRoleLevelAnnot role:public ; vitro:selectFromExistingAnnot  true ; :displayName "has annotation " .
	 local:fpgenconfig11 <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> "edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator"^^<http://www.w3.org/2001/XMLSchema#string> ;
	  <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> "isTargetOf.jsonld";
	 :listViewConfigFile "listViewConfig-isTargetOf.xml"^^xsd:string .
	
		 */
		protected  abstract void generateCustomFormSpecifics(Model appModel);
		protected abstract void generateInstanceCustomFormSpecifics(Model appModel);
	
		
		
		
		private static List<String> retrieveMultipleConfigURIs(String baseURI, String domain, String range, Model appModel) {
			List<String> configURIs = new ArrayList<String>();
			String sparqlQuery = "PREFIX appConfig: <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#> " + 
					   " SELECT ?configContextURI ?configURI WHERE { ?configContextURI appConfig:configContextFor <" + baseURI + "> .";
						sparqlQuery += "?configContextURI appConfig:hasConfiguration ?configURI .";
						sparqlQuery += "?configContextURI appConfig:qualifiedBy <" + range + "> . ";
	
						if(StringUtils.isNotEmpty(domain)) {
							sparqlQuery += "?configContextURI appConfig:qualifiedByDomain <" + domain + "> .";
						}
						
						sparqlQuery += "} ";
						//log.debug(sparqlQuery);
						//Execute this query to retrieve the configURI
						ResultSet rs = executeQuery(appModel, sparqlQuery);
						while(rs.hasNext()) {
							QuerySolution qs = rs.nextSolution();
							if(qs.contains("configURI")) {
								configURIs.add(qs.getResource("configURI").getURI());
							}
						}
			return configURIs;
		}
		
		//this is specifically for sound characteristics
		private String generateConfigRDFForMultipleURIs(List<String> configURIs, String property, String domainURI) {
			String configRDF = "";
			for(String configURI: configURIs) {
				configRDF += generateConfigRDF(configURI, property, domainURI) + "\n";
			}
			return configRDF;
			
		}
	
		protected abstract void generateItemCustomFormSpecifics(Model appModel);
	
		//Putting all this in one method for now
		//Which RDF is actually generated will depend on specifics regarding implementation so abstract method here
		protected abstract String generateConfigRDF(String configURI, String property, String domainURI);
		
		
		//Dropdowns to be hardcode or otherwise used
		protected static void generateCustomFormDropdowns(Model shaclModel,  Model ontologyModel) {
			JSONObject inPropertyLists = new JSONObject();
			JSONObject orPropertyLists = new JSONObject();
			//Test queries
			String sparqlQuery = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
			"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " + 
			"SELECT ?form ?propertyURI ?position ?element WHERE { ?form sh:property ?propInfo . " + 
			"?propInfo sh:path ?propertyURI . " + 
			"?propInfo sh:in ?allowedList . ?allowedList list:index (?position ?element) . } ORDER BY ?form ?propertyURI ?position ?element";
			System.out.println("*******Allowed in list*********");
			try {
				ResultSet rs = executeQuery(shaclModel, sparqlQuery);
				while(rs.hasNext()) {
					QuerySolution qs = rs.nextSolution();
					System.out.println(qs.toString());
					//Get element and propertyURI
					String formURI = qs.getResource("form").getURI();
					String propertyURI = qs.getResource("propertyURI").getURI();
					String elementURI = qs.getResource("element").getURI();
					String key = formURI + "-" + propertyURI;
					if(!inPropertyLists.has(key)) {
						inPropertyLists.put(key, new JSONArray());
					}
					JSONArray inArray = inPropertyLists.getJSONArray(key);
					JSONObject obj = new JSONObject();
					obj.put("uri", elementURI);
					inArray.put(obj);
					
				}
			} catch(Exception ex) {
				System.out.println("Error");
			}
			
			//Query the ontology to get labels for these properties
			
			Iterator k = inPropertyLists.keys();
			while(k.hasNext()) {
				String propFormKey = k.next().toString();
				try {
					JSONArray propArray = inPropertyLists.getJSONArray(propFormKey);
					int len = propArray.length();
					int i;
					for(i = 0; i < len; i++) {
						JSONObject obj = propArray.getJSONObject(i);
						String uri = obj.getString("uri");
						//Create query for this URI and execute query
						String labelQuery = "SELECT ?label ?prefLabel WHERE { OPTIONAL {<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label .} OPTIONAL {<" + uri + "> <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel .} }";
						ResultSet inLabelRS = executeQuery(ontologyModel, labelQuery);
						while(inLabelRS.hasNext()) {
							QuerySolution labelQs = inLabelRS.nextSolution();
							Literal labelLiteral = labelQs.getLiteral("label");
							Literal prefLabelLiteral = labelQs.getLiteral("prefLabel");
							//use pref label if rdfs:label does not exist
							if(labelLiteral == null && prefLabelLiteral != null) labelLiteral = prefLabelLiteral;
							if(labelLiteral != null) {
								String labelLanguage = labelLiteral.getLanguage();
								String inLabel = labelLiteral.getString();
								if(StringUtils.isNotEmpty(inLabel) && (StringUtils.isEmpty(labelLanguage) || labelLanguage.equals("en"))) {
									obj.put("label", inLabel);
								}
							} 
						}
					}
				} catch(Exception ex) {
					System.out.println("error occurred");
				}
			}
			System.out.println(inPropertyLists.toString());
			
			//Do a similar one for sh:or to see what it entails
			String orSparqlQuery = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
					"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " + 
					"SELECT ?form ?propertyURI ?position ?element ?elementp ?elemento WHERE { ?form sh:property ?propInfo . " + 
					"?propInfo sh:path ?propertyURI . " + 
					"?propInfo sh:or ?allowedList . ?allowedList list:index (?position ?element) . ?element ?elementp ?elemento . } ORDER BY ?form ?propertyURI ?position ?elemento";
			System.out.println("********Allowed or list*********");
			try {
				ResultSet orRs = executeQuery(shaclModel, orSparqlQuery);
				while(orRs.hasNext()) {
					QuerySolution qs = orRs.nextSolution();
					System.out.println(qs.toString());
					//Get element and propertyURI
					String formURI = qs.getResource("form").getURI();
					String propertyURI = qs.getResource("propertyURI").getURI();
					String elementURI = qs.getResource("elemento").getURI();
					String key = formURI + "-" + propertyURI;
					if(!orPropertyLists.has(key)) {
						orPropertyLists.put(key, new JSONArray());
					}
					JSONArray inArray = orPropertyLists.getJSONArray(key);
					JSONObject obj = new JSONObject();
					obj.put("uri", elementURI);
					inArray.put(obj);
			
				} 
			} catch(Exception ex) {
				System.out.println("Error occurred");
			}
			//Testing out note thing
			String testUri = "http://audio.biblioteko-o.org/SummaryNote";
			StmtIterator ittest = ontologyModel.listStatements(ResourceFactory.createResource(testUri), null, (RDFNode) null);
			while(ittest.hasNext()) {
				Statement itstmt = ittest.nextStatement();
				System.out.println(itstmt.toString());
			}
			k = orPropertyLists.keys();
			while(k.hasNext()) {
				String propFormKey = k.next().toString();
				try {
					JSONArray propArray = orPropertyLists.getJSONArray(propFormKey);
					int len = propArray.length();
					int i;
					for(i = 0; i < len; i++) {
						JSONObject obj = propArray.getJSONObject(i);
						String uri = obj.getString("uri");
						//Create query for this URI and execute query
						String labelQuery = "SELECT ?label ?prefLabel WHERE { OPTIONAL {<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label .} OPTIONAL {<" + uri + "> <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel .} }";
						ResultSet orLabelRS = executeQuery(ontologyModel, labelQuery);
						while(orLabelRS.hasNext()) {
							QuerySolution labelQs = orLabelRS.nextSolution();
							Literal labelLiteral = labelQs.getLiteral("label");
							Literal prefLabelLiteral = labelQs.getLiteral("prefLabel");
							if(labelLiteral == null && prefLabelLiteral != null) labelLiteral = prefLabelLiteral;
							if(labelLiteral != null) {
								String labelLanguage = labelLiteral.getLanguage();
								String orLabel = labelLiteral.getString();
								if(StringUtils.isNotEmpty(orLabel) && (StringUtils.isEmpty(labelLanguage) || labelLanguage.equals("en"))) {
									obj.put("label", orLabel);
								}
							}
						}
					}
				} catch(Exception ex) {
					System.out.println("error occurred");
				}
			}
			System.out.println(orPropertyLists.toString());
			
			
		}
		
		//Range is assumed to be non-null - remember to check this assumption later
		protected static String retrieveConfigURI(String baseURI, String domain, String range, Model appModel) {
			return retrieveConfigURI(baseURI, domain, range, null, appModel);
		}
		
		//in case domain and range are same across multiple properties, check label
		protected static String retrieveConfigURI(String baseURI, String domain, String range, String label, Model appModel) {
			String sparqlQuery = "PREFIX appConfig: <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#> " + 
		   " SELECT ?configContextURI ?configURI WHERE { ?configContextURI appConfig:configContextFor <" + baseURI + "> .";
			sparqlQuery += "?configContextURI appConfig:hasConfiguration ?configURI .";
			sparqlQuery += "?configContextURI appConfig:qualifiedBy <" + range + "> . ";
	
			if(StringUtils.isNotEmpty(domain)) {
				sparqlQuery += "?configContextURI appConfig:qualifiedByDomain <" + domain + "> .";
			}
			
			if(StringUtils.isNotEmpty(label)) {
				sparqlQuery += "?configURI appConfig:displayName \"" + label + "\" .";
			}
			sparqlQuery += "} ";
			//log.debug(sparqlQuery);
			//Execute this query to retrieve the configURI
			ResultSet rs = executeQuery(appModel, sparqlQuery);
			while(rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				if(qs.contains("configURI")) {
					return qs.getResource("configURI").getURI();
				}
			}
			return null;
		}
		
		private static Model processWork(Model shaclModel) {
			Model appModel = generateWorkModel(shaclModel);
			//Print out app Model
			System.out.println("***********Print out Work MODEL For WORJ**********************");
			//appModel.write(System.out, "N3");
			printOutModel(appModel);		
			System.out.println("*************End Print out Work Model *****************");
			//Comparing to faux properties
			compareToGeneratedProperties(appModel);
			return appModel;
		}
		
		private static Model processInstance(Model shaclModel) {
			Model appModel = generateInstanceModel(shaclModel);
			//Print out app Model
			
			
			System.out.println("***********Print out Instance MODEL For INSTANCE**********************");
			//appModel.write(System.out, "N3");
			//Thank you Jim!
			printOutModel(appModel);				
			System.out.println("*************End Print out Instance Model *****************");
			compareToGeneratedProperties(appModel);
			//System.out.println("*************Generate Template List**************");
			//generateTemplateList(appModel);
			return appModel;
	
		}
		
		private static Model processItem(Model shaclModel) {
			Model appModel = generateItemModel(shaclModel);
			//Print out app Model	
			System.out.println("***********Print out Item MODEL**********************");
			//appModel.write(System.out, "N3");
			//Thank you Jim!
			printOutModel(appModel);		
			System.out.println("*************End Print out Item Model *****************");
			compareToGeneratedProperties(appModel);
			//System.out.println("*************Generate Template List**************");
			//generateTemplateList(appModel);
			return appModel;
	
		}
		
		private static void printOutModel(Model model) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			model.write(stream, "N-TRIPLE");
			String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
			Arrays.sort(lines);
			System.out.println(String.join("\n", lines));
		}
		
		//Load all the ontology files into a model where they can be reviewed/revised
		private static Model getOntologyModel() {
			Model model = readInRDFFilesFromDirectory(ontologyDirectoryPath);	
		    return model;
		}
		
		//Check WHICH SHACL properties NOT defined in current version of ontology files
		private static void compareToOntology(Model shaclModel, Model model) {
		    /*
		    StmtIterator testIt = model.listStatements(ResourceFactory.createResource("http://bibliotek-o.org/ontology/hasActivity"), null, (RDFNode) null);
		    while(testIt.hasNext()) {
		    	System.out.println(testIt.nextStatement().toString());
		    }*/
		    
		    //Shacl Model paths: which of these already exist within the Model
		    List<String> urisMissing = new ArrayList<String>();
		    NodeIterator it = shaclModel.listObjectsOfProperty(ResourceFactory.createProperty("http://www.w3.org/ns/shacl#path"));
		    while(it.hasNext()) {
		    	RDFNode node = it.nextNode();
		    	String uri = node.asResource().getURI();
		    	//System.out.println("uri is for property decorated" + uri);
		    	StmtIterator stmtit = model.listStatements(ResourceFactory.createResource(uri), null, (RDFNode) null);
		    	if(!stmtit.hasNext() && uri != null) {
		    		//System.out.println("This URI does not appear in the ontology files as a subject " + uri);
		    		//there are blank nodes now being used in the case where a path is either not meant to be a field on the form
		    		//or meant to specify an actual path of properties
		    		urisMissing.add(uri);
		    	}
		    }
		    
		    //model.write(System.out, "N3");
		    System.out.println("URIs missing = ");
		    System.out.println(urisMissing.toString());
			
		}
		
		
		private static void generatePropertyGroups(Model shaclModel) {
			//Get all properties for an audio work form
			String typeURI = "http://www.w3.org/ns/shacl#PropertyGroup";
			System.out.println("Before query shape");
			queryAndMapPropertyGroup(typeURI, shaclModel);
			System.out.println("After query shape");
			
			
		}
	
		private static void queryAndMapPropertyGroup(String typeURI, Model shaclModel) {
			String PREFIXES = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
					"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
					"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " + 
					"";
			String query = PREFIXES + 
					"SELECT ?propertyGroup ?propertyGroupLabel ?order ?description WHERE " + 
					"{ ?propertyGroup rdf:type  <" + typeURI + "> .  " + 
					"?propertyGroup rdfs:label ?propertyGroupLabel . " + 
					"?propertyGroup sh:order ?order . " + 
					"OPTIONAL {?propertyGroup sh:description ?description .} " + 
					" } ORDER BY ?order ?propertyGroupLabel";
			/*query = PREFIXES + 
					"SELECT ?property WHERE " + 
					"{ <" + shapeURI + "> sh:property ?property .  " 
					+ " }";
			query = PREFIXES + "SELECT ?s ?p ?o WHERE {?s ?p ?o .}";*/
			System.out.println("Query is " + query);
			ResultSet rs = executeQuery(shaclModel, query);
			//Map results to the RDF for 
			while(rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				
				
				generateVitroLibPropertyGroup(qs);
				
			}
		
		}
	
		private static void generateVitroLibPropertyGroup(QuerySolution qs) {
			// TODO Auto-generated method stub
			Resource pGroupRes = getVarResource(qs, "propertyGroup");
			Literal pGroupLabel = getVarLiteral(qs, "propertyGroupLabel");
			Literal order = getVarLiteral(qs, "order");
			Literal description = getVarLiteral(qs, "description");
			String n3 = "";
			if(pGroupRes != null && pGroupRes.isURIResource() && StringUtils.isNotEmpty(pGroupRes.getURI())) {
				String pGroupResURI = pGroupRes.getURI();
				String uri = "<" + pGroupResURI + ">";
				//Use the same URI to see if it works?
				n3 += uri + " rdf:type <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#PropertyGroup> . ";
				if(pGroupLabel != null && StringUtils.isNotEmpty(pGroupLabel.getString())) {
					n3 += uri + " rdfs:label \"" + pGroupLabel.getString() + "\" . ";
				}
				if(order != null) {
					n3 += uri + " <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#displayRank> \"" + order.getInt() + "\"^^<http://www.w3.org/2001/XMLSchema#integer> . ";
				}
				if(description != null) {
					n3 += uri + " <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#publicDescriptionAnnot> \"" + description.getString() + "\" . ";
				}
				
			}
			
			System.out.println(n3);
			
		}
	
		
	
		private static void compareToGeneratedProperties(Model appModel) {
			System.out.println("Compate to generated properties");
			List<String> existingFauxProperties = new ArrayList<String>();
			// TODO Auto-generated method stub
			File generatedFauxProperties = new File("rdf/generatedFauxProperties.n3");
			Model model= ModelFactory.createDefaultModel();
			try {
				model.read(new FileInputStream(generatedFauxProperties), null, "N3");
				System.out.println("Model read in from generated properties");
			} catch(Exception ex) {
				System.out.println("Error occurred in reading in");
				ex.printStackTrace();
			}
			System.out.println("Read file in and created faux property model");
			
			String configPropURI = "http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#configContextFor";
			NodeIterator it = appModel.listObjectsOfProperty(ResourceFactory.createProperty(configPropURI));
			while(it.hasNext()) {
				RDFNode node = it.next();
				//Is this already in faux property land as 
				Property prop = ResourceFactory.createProperty(configPropURI);
				StmtIterator pIt = model.listStatements(null, prop, node);
				if(pIt.hasNext()) {
					//System.out.println("*****Faux property exists for ********" + node.asResource().getURI());
					existingFauxProperties.add(node.asResource().getURI());
				}
				/*
				while(pIt.hasNext()) {
					System.out.println(pIt.nextStatement());
				}*/
			}
			
			System.out.println("*** Existing faux properties *****");
			System.out.println(existingFauxProperties.toString());
		}
	
		private static Model generateWorkModel(Model shaclModel) {
			
			//Get all properties for an audio work form
			Model appModel = generateAppModel(workFormURI, shaclModel);
			return appModel;
		}
		
		private static Model generateInstanceModel(Model shaclModel) {
			
			//Get all properties for an audio work form
			Model appModel = generateAppModel(instanceFormURI, shaclModel);
			return appModel;
		}
		
		private static Model generateItemModel(Model shaclModel) {
			
			//Get all properties for an audio work form
			Model appModel = generateAppModel(itemFormURI, shaclModel);
			return appModel;
		}
		
		
		private static Model generateAppModel(String shapeURI, Model shaclModel) {
			System.out.println("Before query shape");
			Model appModel = queryShape(shapeURI, shaclModel);
			System.out.println("After query shape");
			return appModel;
		}
		
		//Get all properties and info for an audio work form
		//Need to ensure the ordering stays consistent with the same properties
		private static Model queryShape(String shapeURI, Model queryModel) {
			String PREFIXES = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
					"";
			//Multiple classes possible so grouping together into space separated string here
			//Check for sh:and for a particular property shape as well and then read that in
			String query = PREFIXES + 
					"SELECT ?property ?path ?group ?name ?propDescription ?nodeKind ?order ?target ?orList ?inList ?shapeTarget " + 
					" (group_concat (?iclass) AS ?class) WHERE " + 
					"{ <" + shapeURI + "> sh:property ?property .  " + 
					"?property sh:path ?path . " + 
					"OPTIONAL {?property sh:class ?iclass .} " + 
					"OPTIONAL {?property sh:group ?group .} " + 
					"OPTIONAL {?property sh:name ?name .} " + 
					"OPTIONAL {?property sh:description ?propDescription .} " + 
					"OPTIONAL {?property sh:nodeKind ?nodeKind .} " + 
					"OPTIONAL {?property sh:order ?order .} " + 
					"OPTIONAL {?property sh:target ?target .} " + 
					"OPTIONAL {?property sh:or ?orList .} " + 
					"OPTIONAL {?property sh:in ?inList .} " + 
					"OPTIONAL { <" + shapeURI + "> sh:targetClass ?shapeTarget .} "
					+ " } " + 
					" GROUP BY ?property ?path ?group ?name ?propDescription ?nodeKind ?order ?target ?orList ?inList ?shapeTarget " + 
					" ORDER by ?path ?order ?iclass ?target" ;
					
			System.out.println("Query is " + query);
			ResultSet rs = executeQuery(queryModel, query);
			ResultSetRewindable rsCopy = ResultSetFactory.copyResults(rs);
			Model returnModel = mapToFauxProperties(rsCopy);
			return returnModel;
			
		}
		
		
		//Create a new data property
		//Domain = target
		//Range = class
		
		//Given result set with properties, create new faux properties
		private static Model mapToFauxProperties(ResultSetRewindable rs) {
			rs.reset();
			Model appModel = ModelFactory.createDefaultModel();
			
			// TODO Auto-generated method stub
			try {
				while(rs.hasNext()) {
					QuerySolution qs = rs.nextSolution();
					//System.out.println(qs.toString());
					Model fauxPropModel = createFauxProperty(qs, configNumber);
					appModel.add(fauxPropModel);
					configNumber++;
				} 
			}catch(Exception ex) {
				System.out.println("Exception occurred in getting query solution");
				ex.printStackTrace();
			}
			return appModel;
		}
	
		private static Model createFauxProperty(QuerySolution qs, int configNumber) {
			Model fauxPropertyModel = ModelFactory.createDefaultModel();
			Resource path = getVarResource(qs, "path");
			if(path == null) {
			//If path is null, just return an empty model but do log this
				System.out.println("Path is NULL, QS is " + qs.toString());
				return fauxPropertyModel;
			}
			//If path is not predicate but complex path, do not create a faux property
			//Do print out a message
			//At this point, we know path is NOT null
			String pathURI = path.getURI();
			if(StringUtils.isEmpty(pathURI)) {
				//for now, returning empty model but this could be a property path
				//so will need to deal with that somehow
				return fauxPropertyModel;

			}
			//the path may include the prefix namespace within <>
			//in this case, search for a : that is not the only one
			if(pathURI.lastIndexOf("http:") > pathURI.indexOf("http:") ||
					pathURI.lastIndexOf(":") > pathURI.indexOf(":") ||
					pathURI.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				System.out.println("Faux property not created for " + pathURI);
				return fauxPropertyModel;
			}
			
			String URIType = "http://www.w3.org/ns/shacl#IRI";
			
			//?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList
			
			Literal name = getVarLiteral(qs, "name");
			Literal classLiteral = getVarLiteral(qs, "class"); //this is now space separated
			Resource group = getVarResource(qs, "group");
			Resource nodeKind = getVarResource(qs, "nodeKind");
			Resource target = getVarResource(qs, "target");
			Resource orList = getVarResource(qs, "orList");
			Literal orderLiteral = getVarLiteral(qs, "order");
			Resource shapeTarget = getVarResource(qs, "shapeTarget");
			Literal propDescription = getVarLiteral(qs, "propDescription");
			//Create faux property configuration for this path
			//Starting at 200 because 127 is the last generated faux property
			String prefixes = "@prefix : <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#> .\r\n" + 
					"@prefix vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#> .\r\n" + 
					"@prefix role:  <http://vitro.mannlib.cornell.edu/ns/vitro/role#> .\r\n" + 
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n" + 
					"@prefix bf: <http://id.loc.gov/ontologies/bibframe/> .\r\n" + 
					"@prefix prov: <http://www.w3.org/ns/prov#> .\r\n" + 
					"@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n" + 
					"@prefix oa: <http://www.w3.org/ns/oa#> .\r\n" + 
					"@prefix foaf: <http://xmlns.com/foaf/0.1/> .\r\n" + 
					"@prefix dcterms: <http://purl.org/dc/terms/> .\r\n" + 
					"@prefix local: <http://vitro.mannlib.cornell.edu/ns/vitro/siteConfig/> .\r\n" + 
					"@prefix bib: <http://bibliotek-o.org/ontology/> .\r\n" + 
					"@prefix schema: <http://schema.org/> . ";
			String fauxConfigContextURI = "local:fpgn" + configNumber;
			String fauxConfigURI = "local:fpgenconfig" + configNumber;
			String fauxN3 = prefixes;
			//String pathURI = path.getURI();
			List<String> urisWithoutRange = new ArrayList<String>();
			//IMPORTANT: Switching to nodeKind = IRI if class is present
			//This may not be actually true, so will need to review
			//If object property and there is some kind of range present
			if(nodeKind == null && (classLiteral != null || orList != null)) {
				nodeKind = ResourceFactory.createResource(URIType);
				System.out.println("Setting " + path + " to URI Type");
			}
			if(nodeKind != null) {
				if(nodeKind.getURI().equals(URIType)) {
					fauxN3 += fauxConfigContextURI + " a :ConfigContext ; " + 
							" :configContextFor <" + pathURI + ">;" + 
							" :hasConfiguration " + fauxConfigURI + " . ";
					//class= qualified by, target = qualified by domain
					if(classLiteral != null) {
						//The class value is now space separated in the case of multiple classes
						//this may involve querying the ontology model
						String classURI = getClassURI(classLiteral);
						fauxN3 += fauxConfigContextURI + " :qualifiedBy <" + classURI + "> .";
					} else {
						urisWithoutRange.add(fauxConfigContextURI);
						//Check actual range of property and use that
						//IF no range specified then pick owl:Thing
						String range = getRangeForProperty(pathURI);
						if(StringUtils.isEmpty(range)) {
							range = "owl:Thing";
						} else {
							range = "<" + range + ">";
						}
						fauxN3 += fauxConfigContextURI + " :qualifiedBy " + range + " .";
						//QUESTION: Should a faux property AUTOMATICALLY pick up the ontological range
						//in the case where no range is explicitly specified
	
					}
					if(target != null) {
						fauxN3 += fauxConfigContextURI + " :qualifiedByDomain <" + target.getURI() + "> .";
					} else {
						//If property target IS NULL, check the form target
						if(shapeTarget != null) {
							fauxN3 += fauxConfigContextURI + " :qualifiedByDomain <" + shapeTarget.getURI() + "> .";
						} else {
							System.out.println("**No Domain For:" + fauxConfigContextURI + " - " + pathURI);
						}
					}
					fauxN3 += fauxConfigURI + " a  :ObjectPropertyDisplayConfig ; " + 
					"vitro:collateBySubclassAnnot   false ; vitro:displayLimitAnnot \"0\"^^xsd:int . ";
					if(orderLiteral != null) {
						fauxN3 += fauxConfigURI + " vitro:displayRankAnnot  \"" + orderLiteral.getString() + "\"^^xsd:int .";
					} else {
						//defaulting to order = 0
						fauxN3 += fauxConfigURI + " vitro:displayRankAnnot  \"0\"^^xsd:int .";
					}
					fauxN3 += fauxConfigURI + " vitro:hiddenFromDisplayBelowRoleLevelAnnot role:public ;	vitro:hiddenFromPublishBelowRoleLevelAnnot role:public ; vitro:offerCreateNewOptionAnnot true ; vitro:prohibitedFromUpdateBelowRoleLevelAnnot role:public ; vitro:selectFromExistingAnnot  true .";
					
					if(name != null) {
						fauxN3 += fauxConfigURI + " :displayName \"" + name.getString() +  "\" .";
					}
					
					if(group != null) {
						fauxN3 += fauxConfigURI + " :propertyGroup <" + group.getURI()  +  "> .";
					}
					
					if(propDescription != null) {
						fauxN3 += fauxConfigURI + " vitro:publicDescriptionAnnot \"" + propDescription.getString() + "\"^^xsd:string .";
					}
					
	
				} else {
					System.out.println("DATA PROPERTY:" + pathURI);
				}
			}
			
			//System.out.println("Faux N3 is now " + fauxN3);
			fauxPropertyModel.read(new ByteArrayInputStream(fauxN3.getBytes()), null, "N3");
			if(urisWithoutRange.size() > 0) {
				System.out.println("Properties without range set to owl:Thing or range from ontology");
				System.out.println(StringUtils.join(urisWithoutRange, ","));
			}
			return fauxPropertyModel;
		}
	
		private static String getClassURI(Literal classLiteral) {
			String uri = null;
			String classURIs = classLiteral.getString();
			String[] uris = StringUtils.split(classURIs, " ");
			if(uris.length == 1) {
				uri = uris[0]; 
			} else {
			//If more than one class, we will rely on the ontology to see if one is the subclass of the other
			//i.e. find the most specific class
				uri = findMostSpecificClass(uris);
			}
			return uri;
		}
	
		//Assumption: These are all classes in a hierarchy - if these classes are not related at all
		//then we just return the alphabetical first one.  TODO: Recheck this assumption
		private static String findMostSpecificClass(String[] uris) {
			String classUri = uris[0];
			HashMap<String, String> uriToSuper = new HashMap<String, String>();
			for(String uri: uris) {
				String query = "SELECT (group_concat(?superclass) as ?super) WHERE {<" + uri 
						+ "> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?superclass . } ORDER BY ?superclass";
				ResultSet rs = executeQuery(ontologyModel, query);
				while(rs.hasNext()) {
					QuerySolution qs = rs.nextSolution();
					String superuri = qs.getLiteral("super").getString();
					uriToSuper.put(uri,superuri);
				}
				
			}
			//If all of the rest of the uris have been found as superclasses for a uri, that is the most specific class
			//Note: there will be more elegant ways to do this
			for(String uri:uris) {
				String superuri = uriToSuper.get(uri);
				boolean foundUri = true;
				for(String remUri: uris) {
					if(remUri != uri && !superuri.contains(remUri)) {
						foundUri = false;
						break;
					}
				}
				//all of the other uris are superclasses of this uri, so this is the most specific subclass
				if(foundUri) {
					classUri = uri;
				}
			}
			return classUri;
		}
		
		//Find the current range uri in the ontology
		private static String getRangeForProperty(String propertyUri) {
			String query = "SELECT ?range WHERE {<" + propertyUri + "> <http://www.w3.org/2000/01/rdf-schema#range> ?range .}";
			ResultSet rs = executeQuery(ontologyModel, query);
			while(rs.hasNext()) {
				return rs.nextSolution().getResource("range").getURI();
			}
			
			return null;
			
		}
	
	
		private static Literal getVarLiteral(QuerySolution qs, String varName) {
			if(qs.get(varName) != null && qs.get(varName).isLiteral()) {
				return qs.getLiteral(varName);
			}
			return null;
		}
		
		private static Resource getVarResource(QuerySolution qs, String varName) {
			if(qs.get(varName) != null && qs.get(varName).isResource()) {
				return qs.getResource(varName);
			}
			return null;
		}
		
		private static ResultSet executeQuery(Model queryModel, String query) {
			Query q= QueryFactory.create(query);
			ResultSet rs = null;
			QueryExecution qe = QueryExecutionFactory.create(q, queryModel);
			try {
				rs = qe.execSelect();
				
			} catch(Exception ex) {
				System.out.println("Error executing this query");
			}
			return rs;
		}
		
		private static Model executeConstructQuery(Model queryModel, String query) {
			Query q= QueryFactory.create(query);
			Model results = null;
			QueryExecution qe = QueryExecutionFactory.create(q, queryModel);
			try {
				results = qe.execConstruct();
				
			} catch(Exception ex) {
				System.out.println("Error executing this query");
			}
			return results;
		}
	
	
		//Read in SHACL files and populate model
		private static Model populateModel() {
			Model model= readInRDFFilesFromDirectory(shaclDirectoryPath);
			//File shaclFile = new File("rdf/bibliotek-o_shapes.shapes.ttl");	
			//Let's normalize this model too, i.e .to some extent
			//Where sh:node is USED, copy over those predicates to the main property shape
			model = normalizeNodes(model);
			System.out.println("***Normalize Model****");
			model.write(System.out, "N3");
			System.out.println("****End normalized model******");
			return model;
		}
		    
	    private static Model normalizeNodes(Model model) {
	    	System.out.println("Normalize model!");
	    	Property nodeProperty = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#node");
			StmtIterator stit = model.listStatements(null, nodeProperty, (RDFNode) null);
			//stop recursing at this point if we have no more sh:node properties
			if(!stit.hasNext()) {
				System.out.println("Stopping recursing for normalizing model!");
				return model;
			} 
			
			Model normalizedModel = ModelFactory.createDefaultModel();
	    	normalizedModel.add(model);
			while(stit.hasNext()) {
				Statement stmt = stit.nextStatement();
				Resource subjectResource = stmt.getSubject();
				Resource objectResource = stmt.getResource();
				//Get ALL the stmts from the object resource and apply them to the subject resource
				StmtIterator oit = model.listStatements(objectResource, null, (RDFNode) null);
				while(oit.hasNext()) {
					Statement oStmt = oit.nextStatement();
					//System.out.println("Adding stmt to subject instead" + oStmt.toString());
					normalizedModel.add(subjectResource, oStmt.getPredicate(), oStmt.getObject());
					//for the recursion to actually stop, we need to remove the sh:node statements
					//once we've 'walked the graph' properly
				}
				//remove sh:node statement
				normalizedModel.remove(stmt);
			}
			//call recursive method
			return normalizeNodes(normalizedModel);
		}

		//Read in ontology files or SHACL files from a directory
		//This could also be a nested directory so will try and read from those as well
		private static Model readInRDFFilesFromDirectory(String directoryPath) {
			Model model= ModelFactory.createDefaultModel();
			
			//Read in from a specific directory instead of just a file
			File directory = new File(directoryPath);
		    for(File fileEntry : directory.listFiles()) {
				try {
					if(fileEntry.isDirectory()) {
						Model directoryModel = readInRDFFilesFromDirectory(fileEntry.getAbsolutePath());
						model.add(directoryModel);
					} else {
						FileInputStream fis = (new FileInputStream(fileEntry));
						String fn = fileEntry.getName();
						//String fn = Paths.get(fileEntry.getPath()).getFileName().toString().toLowerCase();
						//System.out.println("Reading in " + fn);
						 //String fn = fileEntry.getPath().getFileName()
		                 if ( fn.endsWith(".nt") ) {
		                     model.read( fis, null, "N-TRIPLE" );
		                 } else if ( fn.endsWith(".n3") ) {
		                     model.read(fis, null, "N3");
		                 } else if ( fn.endsWith(".ttl") ) {
		                     model.read(fis, null, "TURTLE");
		                 } else if ( fn.endsWith(".owl") || fn.endsWith(".rdf") || fn.endsWith(".xml") ) {
		                	 System.out.println("Reading in owl, rdf, xml" + fn);
		                     model.read( fis, null, "RDF/XML" );
		                 } else if ( fn.endsWith(".md") ) {
		                 	// Ignore markdown files - documentation.
		                 } else {
		                     //log.warn("Ignoring " + type + " file graph " + p + " because the file extension is unrecognized.");
		                 }
					}
	                 
	                 
				} catch(Exception ex) {
					System.out.println("Error occurred in reading in SHACL files: " + fileEntry.getName());
					ex.printStackTrace();
				}
		    }
		
			System.out.println("Read file in and populated model");
			return model;
		
		}
	}
	
	
	
}