import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;


public class TranslateShacl {
	
	public static void main (String[] args) {
		
		Model shaclModel = populateModel();
		Model appModel = generateAppModel(shaclModel);
		//Print out app Model
		
		
		System.out.println("Print out APP MODEL");
		//appModel.write(System.out, "N3");
		//Thank you Jim!
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		appModel.write(stream, "N-TRIPLE");
		String[] lines = new String(stream.toByteArray()).split("[\\n\\r]");
		Arrays.sort(lines);
		System.out.println(String.join("\n", lines));

		
		//System.out.println(appModel.listStatements().toList().stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
		
		//Check against generated faux properties to see which properties are 
		
		System.out.println("******");
		
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
		
		
	}

	private static Model generateAppModel(Model shaclModel) {
		
		//Get all properties for an audio work form
		String shapeURI = "http://example.org/bibliotek-o_shapes#AudioWorkForm";
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
				"SELECT ?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList WHERE " + 
				"{ <" + shapeURI + "> sh:property ?property .  " + 
				"?property sh:path ?path . " + 
				"OPTIONAL {?property sh:class ?class .} " + 
				"OPTIONAL {?property sh:group ?group .} " + 
				"OPTIONAL {?property sh:name ?name .} " + 
				"OPTIONAL {?property sh:nodeKind ?nodeKind .} " + 
				"OPTIONAL {?property sh:order ?order .} " + 
				"OPTIONAL {?property sh:target ?target .} " + 
				"OPTIONAL {?property sh:or ?orList .} "
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
		//If object property
		if(nodeKind != null) {
			if(nodeKind.getURI().equals(URIType)) {
				fauxN3 += fauxConfigContextURI + " a :ConfigContext ; " + 
						" :configContextFor <" + pathURI + ">;" + 
						" :hasConfiguration " + fauxConfigURI + " . ";
				//class= qualified by, target = qualified by domain
				if(classResource != null) {
					fauxN3 += fauxConfigContextURI + " :qualifiedBy <" + classResource.getURI() + "> .";
				}
				if(target != null) {
					fauxN3 += fauxConfigContextURI + " :qualifiedByDomain <" + target.getURI() + "> .";
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
				

			} else {
				
			}
		}
		
		System.out.println("Faux N3 is now " + fauxN3);
		fauxPropertyModel.read(new ByteArrayInputStream(fauxN3.getBytes()), null, "N3");
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

	//Read in SHACL files and populate model
	private static Model populateModel() {
		File shaclFile = new File("rdf/shacl.ttl");
		
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