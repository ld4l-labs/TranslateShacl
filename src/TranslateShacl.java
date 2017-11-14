import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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


public class TranslateShacl {
	
	public static void main (String[] args) {
		
		Model shaclModel = populateModel();
		
		compareToOntology(shaclModel);
		
		Model appModel = generateWorkModel(shaclModel);
		//Print out app Model
		
		//Generate property groups
		System.out.println("********Generate Property Groups***********");
		generatePropertyGroups(shaclModel);
		
		System.out.println("***********Print out APP MODEL For WORJ**********************");
		//appModel.write(System.out, "N3");
		//Thank you Jim!
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		appModel.write(stream, "N-TRIPLE");
		String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
		Arrays.sort(lines);
		System.out.println(String.join("\n", lines));

		
		//System.out.println(appModel.listStatements().toList().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
		
		//Check against generated faux properties to see which properties are 
		
		System.out.println("*************End Print out APP Model *****************");
		compareToGeneratedProperties(appModel);
		System.out.println("*************Generate Template List**************");
		generateTemplateList(appModel);

		
		
		
		
	}

	//Check WHICH SHACL properties NOT defined in current version of ontology files
	private static void compareToOntology(Model shaclModel) {
		//Read in all the files
    	Model model= ModelFactory.createDefaultModel();

		File directory = new File("rdf/currentOntologyFiles");
	    for(File fileEntry : directory.listFiles()) {
			try {
				FileInputStream fis = (new FileInputStream(fileEntry));
				String fn = fileEntry.getName();
				//String fn = Paths.get(fileEntry.getPath()).getFileName().toString().toLowerCase();
				System.out.println("Reading in " + fn);
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
	    
	    StmtIterator testIt = model.listStatements(ResourceFactory.createResource("http://bibliotek-o.org/ontology/hasActivity"), null, (RDFNode) null);
	    while(testIt.hasNext()) {
	    	System.out.println(testIt.nextStatement().toString());
	    }
	    //Shacl Model paths: which of these already exist within the Model
	    NodeIterator it = shaclModel.listObjectsOfProperty(ResourceFactory.createProperty("http://www.w3.org/ns/shacl#path"));
	    while(it.hasNext()) {
	    	RDFNode node = it.nextNode();
	    	String uri = node.asResource().getURI();
	    	System.out.println("uri is for property decorated" + uri);
	    	StmtIterator stmtit = model.listStatements(ResourceFactory.createResource(uri), null, (RDFNode) null);
	    	if(!stmtit.hasNext()) {
	    		System.out.println("This URI does not appear in the ontology files as a subject " + uri);
	    	}
	    }
	    
	    model.write(System.out, "N3");
		
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
				System.out.println("*****Faux property exists for ********" + node.asResource().getURI());
			}
			while(pIt.hasNext()) {
				System.out.println(pIt.nextStatement());
			}
		}
	}

	private static Model generateWorkModel(Model shaclModel) {
		
		//Get all properties for an audio work form
		String shapeURI = "http://example.org/bibliotek-o_shapes#AudioWorkForm";
		Model appModel = generateAppModel(shapeURI, shaclModel);
		return appModel;
	}
	
	private static Model generateInstanceModel(Model shaclModel) {
		
		//Get all properties for an audio work form
		String shapeURI = "http://example.org/bibliotek-o_shapes#AudioInstanceForm";
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
				"SELECT ?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList ?shapeTarget WHERE " + 
				"{ <" + shapeURI + "> sh:property ?property .  " + 
				"?property sh:path ?path . " + 
				"OPTIONAL {?property sh:class ?class .} " + 
				"OPTIONAL {?property sh:group ?group .} " + 
				"OPTIONAL {?property sh:name ?name .} " + 
				"OPTIONAL {?property sh:nodeKind ?nodeKind .} " + 
				"OPTIONAL {?property sh:order ?order .} " + 
				"OPTIONAL {?property sh:target ?target .} " + 
				"OPTIONAL {?property sh:or ?orList .} " + 
				"OPTIONAL { <" + shapeURI + "> sh:target ?shapeTarget .} "
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
		int configNumber = 200;
		// TODO Auto-generated method stub
		try {
			while(rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				System.out.println(qs.toString());
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
		String URIType = "http://www.w3.org/ns/shacl#IRI";
		Model fauxPropertyModel = ModelFactory.createDefaultModel();
		//?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList
		Resource path = getVarResource(qs, "path");
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
		//If object property
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
		
		System.out.println("Faux N3 is now " + fauxN3);
		fauxPropertyModel.read(new ByteArrayInputStream(fauxN3.getBytes()), null, "N3");
		System.out.println("Faux properties without range");
		System.out.println(StringUtils.join(urisWithoutRange, ","));
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
		File shaclFile = new File("rdf/shacl2.ttl");
		
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