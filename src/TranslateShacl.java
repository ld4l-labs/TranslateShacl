import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.JSONArray;
import org.json.JSONObject;


public class TranslateShacl {
	public static int configNumber = 200;
	public static void main (String[] args) {
		
		Model shaclModel = populateModel();
		Model ontologyModel = getOntologyModel();
		compareToOntology(shaclModel, ontologyModel);
		//Generate property groups
		System.out.println("********Generate Property Groups***********");
		generatePropertyGroups(shaclModel);
		System.out.println("********Generate Work*********");
		//Work level info		
		processWork(shaclModel, ontologyModel);
		System.out.println("********Generate Instance*********");
		//Instance level info
		processInstance(shaclModel);
		System.out.println("********Generate Item*********");

		//Item level info
		processItem(shaclModel);
		//Generate dropdowns
		generateCustomFormDropdowns(shaclModel, ontologyModel);

		
		
		
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
	private static void generateCustomFormSpecifics(Model appModel) {
		//App Model uses the vitroLib-specific faux configuration
		//TODO: Make this use classes instead
		//Base URI -> <domain =, range= >
		String rdfString = "";
		//has part custom form
		String configURI = retrieveConfigURI("http://purl.org/dc/terms/hasPart", "http://id.loc.gov/ontologies/bibframe/Audio", "http://id.loc.gov/ontologies/bibframe/Audio", appModel);
		rdfString += generateConfigRDF(configURI, "hasPart") + "\n";
		
		//Genre form, audio to concept
		configURI =  retrieveConfigURI("http://id.loc.gov/ontologies/bibframe/genreForm", "http://id.loc.gov/ontologies/bibframe/Audio", "http://www.w3.org/2004/02/skos/core#Concept", appModel);
		rdfString += generateConfigRDF(configURI, "genreForm")+ "\n";
		//has activity
		configURI =  retrieveConfigURI("http://bibliotek-o.org/ontology/hasActivity", "http://id.loc.gov/ontologies/bibframe/Audio", "http://bibliotek-o.org/ontology/Activity", appModel);
		rdfString += generateConfigRDF(configURI, "hasActivity")+ "\n";
		//Subject
		//http://www.w3.org/2002/07/owl#
		configURI =  retrieveConfigURI("http://purl.org/dc/terms/subject", "http://id.loc.gov/ontologies/bibframe/Audio", "http://www.w3.org/2002/07/owl#Thing", appModel);
		rdfString += generateConfigRDF(configURI, "subject")+ "\n";
		System.out.println(rdfString);
	}
	
	//Putting all this in one method for now
	private static String generateConfigRDF(String configURI, String property) {
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
				//These may be different, as in different faux properties for work has activity and instance has activity
				
				configRDF =  "<" + configURI + "> :listViewConfigFile \"listViewConfig-workHasActivity.xml\"^^xsd:string . " + 
							"<" + configURI + ">  <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ;" + 
							"<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"workHasActivity.jsonld\" . ";
					 
				//+  both require list views and custom forms 
				//"<" + configURI + "> :listViewConfigFile \"listViewConfig-instanceHasActivity.xml\"^^xsd:string .  " + 
						 

				break;	
			case "subject":
				configRDF = "<" + configURI + "> :listViewConfigFile \"listViewConfig-subject.xml\"^^xsd:string .  ";
				configRDF += "<" + configURI + "> <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot> \"edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator\"^^<http://www.w3.org/2001/XMLSchema#string> ; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot> \"hasLCSH.jsonld\"; " + 
						 "<http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot> \"hasLCSH.ftl\" .";

				break;
			default:
				break;
		}
		return configRDF;
	}
	
	//Dropdowns to be hardcode or otherwise used
	private static void generateCustomFormDropdowns(Model shaclModel,  Model ontologyModel) {
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
					String labelQuery = "SELECT ?label WHERE {<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label .}";
					ResultSet inLabelRS = executeQuery(ontologyModel, labelQuery);
					while(inLabelRS.hasNext()) {
						QuerySolution labelQs = inLabelRS.nextSolution();
						String inLabel = labelQs.getLiteral("label").getString();
						if(StringUtils.isNotEmpty(inLabel)) {
							obj.put("label", inLabel);
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
					String labelQuery = "SELECT ?label WHERE {<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label .}";
					ResultSet orLabelRS = executeQuery(ontologyModel, labelQuery);
					while(orLabelRS.hasNext()) {
						QuerySolution labelQs = orLabelRS.nextSolution();
						String orLabel = labelQs.getLiteral("label").getString();
						if(StringUtils.isNotEmpty(orLabel)) {
							obj.put("label", orLabel);
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
	private static String retrieveConfigURI(String baseURI, String domain, String range, Model appModel) {
		String sparqlQuery = "PREFIX appConfig: <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#> " + 
	   " SELECT ?configContextURI ?configURI WHERE { ?configContextURI appConfig:configContextFor <" + baseURI + "> .";
		sparqlQuery += "?configContextURI appConfig:hasConfiguration ?configURI .";
		sparqlQuery += "?configContextURI appConfig:qualifiedBy <" + range + "> . ";
		if(StringUtils.isNotEmpty(domain)) {
			sparqlQuery += "?configContextURI appConfig:qualifiedByDomain <" + domain + "> .";
		}
		sparqlQuery += "} ";
		System.out.println(sparqlQuery);
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
	
	private static void processWork(Model shaclModel, Model ontologyModel) {
		Model appModel = generateWorkModel(shaclModel);
		//Print out app Model
		
		
		System.out.println("***********Print out Work MODEL For WORJ**********************");
		//appModel.write(System.out, "N3");
		//Thank you Jim!
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		appModel.write(stream, "N-TRIPLE");
		String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
		Arrays.sort(lines);
		System.out.println(String.join("\n", lines));

		
		//System.out.println(appModel.listStatements().toList().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
		
		//Check against generated faux properties to see which properties are 
		
		System.out.println("*************End Print out Work Model *****************");
		//Comparing to faux properties
		compareToGeneratedProperties(appModel);
		//No longer generating this as not hard-coding anymore
		//System.out.println("*************Generate Template List**************");
		//generateTemplateList(appModel);
		//retrieve which generated config uris will match certain conditions in order to associate custom form/list view info
		generateCustomFormSpecifics(appModel);
	}
	
	private static void processInstance(Model shaclModel) {
		Model appModel = generateInstanceModel(shaclModel);
		//Print out app Model
		
		
		System.out.println("***********Print out Instance MODEL For INSTANCE**********************");
		//appModel.write(System.out, "N3");
		//Thank you Jim!
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		appModel.write(stream, "N-TRIPLE");
		String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
		Arrays.sort(lines);
		System.out.println(String.join("\n", lines));

		
		//System.out.println(appModel.listStatements().toList().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
		
		//Check against generated faux properties to see which properties are 
		
		System.out.println("*************End Print out Instance Model *****************");
		compareToGeneratedProperties(appModel);
		System.out.println("*************Generate Template List**************");
		generateTemplateList(appModel);

	}
	
	private static void processItem(Model shaclModel) {
		Model appModel = generateItemModel(shaclModel);
		//Print out app Model
		
		
		System.out.println("***********Print out Item MODEL**********************");
		//appModel.write(System.out, "N3");
		//Thank you Jim!
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		appModel.write(stream, "N-TRIPLE");
		String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
		Arrays.sort(lines);
		System.out.println(String.join("\n", lines));

		
		//System.out.println(appModel.listStatements().toList().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
		
		//Check against generated faux properties to see which properties are 
		
		System.out.println("*************End Print out Item Model *****************");
		compareToGeneratedProperties(appModel);
		System.out.println("*************Generate Template List**************");
		generateTemplateList(appModel);

	}
	
	//Load all the ontology files into a model where they can be reviewed/revised
	private static Model getOntologyModel() {
		//Read in all the files
    	Model model= ModelFactory.createDefaultModel();

		File directory = new File("rdf/currentOntologyFiles");
	    for(File fileEntry : directory.listFiles()) {
			try {
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
                 
                 
			} catch(Exception ex) {
				System.out.println("Error occurred in reading in");
				ex.printStackTrace();
			}
            
        }
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
	    	if(!stmtit.hasNext()) {
	    		//System.out.println("This URI does not appear in the ontology files as a subject " + uri);
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

	private static void generateTemplateList(Model appModel) {
		/*Need to generate THIS based on the shacl file
		 * Can we call in instance view into the work view somehow?
		 * <#assign propertyInfoList = [ {"baseUri":"http://bibliotek-o.org/ontology/hasActivity", "rangeUri":"http://bibliotek-o.org/ontology/Activity"},
		{"baseUri":"http://id.loc.gov/ontologies/bibframe/hasInstance"},
		{"baseUri":"http://purl.org/dc/terms/subject", "rangeUri":"http://www.w3.org/2002/07/owl#Thing", "domainUri":"http://id.loc.gov/ontologies/bibframe/Work"},
		{"baseUri":"http://id.loc.gov/ontologies/bibframe/genreForm", "rangeUri":"http://id.loc.gov/ontologies/bibframe/GenreForm", "domainUri":"http://id.loc.gov/ontologies/bibframe/Work"},
		{"baseUri":"http://purl.org/dc/terms/hasPart", "rangeUri":"http://id.loc.gov/ontologies/bibframe/Work","domainUri":"http://id.loc.gov/ontologies/bibframe/Work"},
		{"baseUri":"http://bibliotek-o.org/ontology/isTargetOf", "rangeUri":"http://www.w3.org/ns/oa#Annotation","domainUri":"http://id.loc.gov/ontologies/bibframe/Work"}
		] />
		 * 
		 */
		
		//Based on ORDER IF SPECIFIED: 
		//AND THEN the rest should be sorted alphabetically
		
		String query = "SELECT ?uri ?domain ?range ?rank WHERE {" +
		"?configContext <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#configContextFor> ?uri . " +
		"?configContext <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#hasConfiguration> ?config . " +
		"OPTIONAL {?configContext <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedBy> ?range . } " +
		"OPTIONAL {?configContext <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedByDomain> ?domain . } " + 
		"OPTIONAL {?config <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#displayRankAnnot> ?rank . }" + 
		"  } ORDER BY ?rank ?uri";
		System.out.println(query);
		
		ResultSet rs = executeQuery(appModel, query);
		while(rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource uriResource = getVarResource(qs, "uri");
			Resource domainResource = getVarResource(qs, "domain");
			Resource rangeResource = getVarResource(qs, "range");
			Literal rankLiteral = getVarLiteral(qs, "rank");
			/*
			 * 		{"baseUri":"http://purl.org/dc/terms/subject", "rangeUri":"http://www.w3.org/2002/07/owl#Thing", "domainUri":"http://id.loc.gov/ontologies/bibframe/Work"},

			 */
			String toPrint = "{\"baseUri\":\"" + uriResource.getURI() + "\"";
			if(domainResource != null) {
				toPrint += 	", \"domainUri\":\"" + domainResource.getURI() + "\"";
			}
			if(rangeResource != null) {
				toPrint += ", \"rangeUri\":\"" +  rangeResource.getURI() + "\"";
			}
			toPrint += "}";
			//if(rankLiteral != null) {toPrint += rankLiteral.getString();}
			System.out.println(toPrint);
			
		}
		
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
		String shapeURI = "http://bibliotek-o.org/shapes/audio/AudioWorkForm";
		Model appModel = generateAppModel(shapeURI, shaclModel);
		return appModel;
	}
	
	private static Model generateInstanceModel(Model shaclModel) {
		
		//Get all properties for an audio work form
		String shapeURI = "http://bibliotek-o.org/shapes/audio/AudioInstanceForm";
		Model appModel = generateAppModel(shapeURI, shaclModel);
		return appModel;
	}
	
	private static Model generateItemModel(Model shaclModel) {
		
		//Get all properties for an audio work form
		String shapeURI = "http://bibliotek-o.org/shapes/audio/AudioItemForm";
		Model appModel = generateAppModel(shapeURI, shaclModel);
		return appModel;
	}
	
	
	private static Model generateAppModel(String shapeURI, Model shaclModel) {
		System.out.println("Before query shape");
		Model appModel = queryShape(shapeURI, shaclModel);
		System.out.println("After query shape");
		return appModel;
	}
	
	//Get all properties and info for an audio work form
	private static Model queryShape(String shapeURI, Model queryModel) {
		String PREFIXES = "PREFIX sh: <http://www.w3.org/ns/shacl#> " + 
				"";
		String query = PREFIXES + 
				"SELECT ?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList ?inList ?shapeTarget WHERE " + 
				"{ <" + shapeURI + "> sh:property ?property .  " + 
				"?property sh:path ?path . " + 
				"OPTIONAL {?property sh:class ?class .} " + 
				"OPTIONAL {?property sh:group ?group .} " + 
				"OPTIONAL {?property sh:name ?name .} " + 
				"OPTIONAL {?property sh:nodeKind ?nodeKind .} " + 
				"OPTIONAL {?property sh:order ?order .} " + 
				"OPTIONAL {?property sh:target ?target .} " + 
				"OPTIONAL {?property sh:or ?orList .} " + 
				"OPTIONAL {?property sh:in ?inList .} " + 
				"OPTIONAL { <" + shapeURI + "> sh:targetClass ?shapeTarget .} "
				+ " }";
		/*query = PREFIXES + 
				"SELECT ?property WHERE " + 
				"{ <" + shapeURI + "> sh:property ?property .  " 
				+ " }";
		query = PREFIXES + "SELECT ?s ?p ?o WHERE {?s ?p ?o .}";*/
		System.out.println("Query is " + query);
		ResultSet rs = executeQuery(queryModel, query);
		Model returnModel = mapToFauxProperties(rs);
	
		return returnModel;
		
	}
	
	
	//Given result set with properties, create new faux properties
	private static Model mapToFauxProperties(ResultSet rs) {
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
		//If path is not predicate but complex path, do not create a faux property
		//Do print out a message
		if(path != null) {
			String pathURI = path.getURI();
			//the path may include the prefix namespace within <>
			//in this case, search for a : that is not the only one
			if(pathURI.lastIndexOf("http:") > pathURI.indexOf("http:") ||
					pathURI.lastIndexOf(":") > pathURI.indexOf(":")) {
				System.out.println("Faux property not created for " + pathURI);
				return fauxPropertyModel;
			}
		}
		String URIType = "http://www.w3.org/ns/shacl#IRI";
		
		//?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList
		
		Literal name = getVarLiteral(qs, "name");
		Resource classResource = getVarResource(qs, "class");
		Resource group = getVarResource(qs, "group");
		Resource nodeKind = getVarResource(qs, "nodeKind");
		Resource target = getVarResource(qs, "target");
		Resource orList = getVarResource(qs, "orList");
		Literal orderLiteral = getVarLiteral(qs, "order");
		Resource shapeTarget = getVarResource(qs, "shapeTarget");
		
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
		String pathURI = path.getURI();
		List<String> urisWithoutRange = new ArrayList<String>();
		//IMPORTANT: Switching to nodeKind = IRI if class is present
		//This may not be actually true, so will need to review
		//If object property and there is some kind of range present
		if(nodeKind == null && (classResource != null || orList != null)) {
			nodeKind = ResourceFactory.createResource(URIType);
			System.out.println("Setting " + path + " to URI Type");
		}
		if(nodeKind != null) {
			if(nodeKind.getURI().equals(URIType)) {
				fauxN3 += fauxConfigContextURI + " a :ConfigContext ; " + 
						" :configContextFor <" + pathURI + ">;" + 
						" :hasConfiguration " + fauxConfigURI + " . ";
				//class= qualified by, target = qualified by domain
				if(classResource != null) {
					fauxN3 += fauxConfigContextURI + " :qualifiedBy <" + classResource.getURI() + "> .";
				} else {
					urisWithoutRange.add(fauxConfigContextURI);
					//For now, simply to make this work in the interface when we upload it, changing this to owl:Thing
					//As faux properties require ranges
					fauxN3 += fauxConfigContextURI + " :qualifiedBy owl:Thing .";
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
				

			} else {
				
			}
		}
		
		//System.out.println("Faux N3 is now " + fauxN3);
		fauxPropertyModel.read(new ByteArrayInputStream(fauxN3.getBytes()), null, "N3");
		if(urisWithoutRange.size() > 0) {
			System.out.println("Properties without range set to owl:Thing");
			System.out.println(StringUtils.join(urisWithoutRange, ","));
		}
		return fauxPropertyModel;
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
		//shacl.ttl has the copy from Steven
		File shaclFile = new File("rdf/bibliotek-o_shapes.shapes.ttl");
		
		Model model= ModelFactory.createDefaultModel();
		try {
			model.read(new FileInputStream(shaclFile), null, "TTL");
		} catch(Exception ex) {
			System.out.println("Error occurred in reading in");
			ex.printStackTrace();
		}
		System.out.println("Read file in and populated model");
		return model;
		
		
	}
	
	
}