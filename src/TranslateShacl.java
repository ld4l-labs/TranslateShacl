import java.io.File;
import java.io.FileInputStream;

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
				"OPTIONAL {?property sh:targer ?target .} " + 
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
	
		// TODO Auto-generated method stub
		try {
			while(rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				System.out.println(qs.toString());
				Model fauxPropModel = createFauxProperty(qs);
				appModel.add(fauxPropModel);
			} 
		}catch(Exception ex) {
			System.out.println("Exception occurred in getting query solution");
		}
		return null;
	}

	private static Model createFauxProperty(QuerySolution qs) {
		Model fauxPropertyModel = ModelFactory.createDefaultModel();
		//?property ?path ?class ?group ?name ?nodeKind ?order ?target ?orList
		Resource path = getVarResource(qs, "path");
		Literal name = getVarLiteral(qs, "name");
		Resource classResource = getVarResource(qs, "class");
		Resource group = getVarResource(qs, "group");
		Resource nodeKind = getVarResource(qs, "nodeKind");
		Resource target = getVarResource(qs, "target");
		Resource orList = getVarResource(qs, "orList");
		
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