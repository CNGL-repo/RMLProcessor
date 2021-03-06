package be.ugent.mmlab.rml.processor;

import static be.ugent.mmlab.rml.model.TermType.BLANK_NODE;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import be.ugent.mmlab.rml.core.ConditionalJoinRMLPerformer;
import be.ugent.mmlab.rml.core.JoinRMLPerformer;
import be.ugent.mmlab.rml.core.RMLEngine;
import be.ugent.mmlab.rml.core.RMLPerformer;
import be.ugent.mmlab.rml.core.SimpleReferencePerformer;
import be.ugent.mmlab.rml.model.GraphMap;
import be.ugent.mmlab.rml.model.JoinCondition;
import be.ugent.mmlab.rml.model.LogicalSource;
import be.ugent.mmlab.rml.model.ObjectMap;
import be.ugent.mmlab.rml.model.PredicateMap;
import be.ugent.mmlab.rml.model.PredicateObjectMap;
import be.ugent.mmlab.rml.model.ReferencingObjectMap;
import be.ugent.mmlab.rml.model.SubjectMap;
import be.ugent.mmlab.rml.model.TermMap;
import be.ugent.mmlab.rml.model.TriplesMap;
import be.ugent.mmlab.rml.model.reference.ReferenceIdentifierImpl;
import be.ugent.mmlab.rml.processor.concrete.ConcreteRMLProcessorFactory;
import be.ugent.mmlab.rml.vocabulary.Vocab.QLTerm;
import function.JSEnv;
import net.antidot.semantic.rdf.model.impl.sesame.SesameDataSet;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.core.R2RMLEngine;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.exception.R2RMLDataError;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.model.TermType;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.tools.R2RMLToolkit;

/**
 * This class contains all generic functionality for executing an iteration and
 * processing the mapping
 *
 * @author mielvandersande, andimou
 */
public abstract class AbstractRMLProcessor implements RMLProcessor {

    /**
     * Gets the globally defined identifier-to-path map
     *
     * @param ls the current LogicalSource
     * @return the location of the file or table
     */
    // Log
    private static Log log = LogFactory.getLog(R2RMLEngine.class);

    /*protected String getIdentifier(LogicalSource ls) {
        return RMLEngine.getFileMap().getProperty(ls.getIdentifier());
    }*/

    /**
     * gets the expression specified in the logical source
     *
     * @param ls
     * @return
     */
    protected String getReference(LogicalSource ls) {
        return ls.getReference();
    }

    /**
     *
     * Process the subject map
     *
     * @param dataset
     * @param subjectMap
     * @param node
     * @return the created subject
     */
    @Override
    public Resource processSubjectMap(SesameDataSet dataset, SubjectMap subjectMap, Object node) {       
        //Get the uri
        List<String> values = processTermMap(subjectMap, node);    
        //log.info("Abstract RML Processor Graph Map" + subjectMap.getGraphMaps().toString());
        if (values.isEmpty()) 
            if(subjectMap.getTermType() != BLANK_NODE)
                return null;
            
        String value = null;
        if(subjectMap.getTermType() != BLANK_NODE){
            //Since it is the subject, more than one value is not allowed. 
            //Only return the first one. Throw exception if not?
            value = values.get(0);

            if ((value == null) || (value.equals(""))) 
                return null;
        }
        
        Resource subject = null;
               
        //doublicate code from ObjectMap - they should be handled together
        switch (subjectMap.getTermType()) {
            case IRI:
                if (value != null && !value.equals("")){
                    if(value.startsWith("www."))
                        value = "http://" + value;
                    subject = new URIImpl(value);
                }
                break;
            case BLANK_NODE:
                subject = new BNodeImpl(org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(10));
                break;
            default:
                subject = new URIImpl(value);
        }
        //subject = new URIImpl(value);
        return subject;
    }
        
    @Override
    public void processSubjectTypeMap(SesameDataSet dataset, Resource subject, SubjectMap subjectMap, Object node) {

        //Add the type triples
        Set<org.openrdf.model.URI> classIRIs = subjectMap.getClassIRIs();
        if(subject != null)
            for (org.openrdf.model.URI classIRI : classIRIs) 
                if(subjectMap.getGraphMaps().isEmpty())
                    dataset.add(subject, RDF.TYPE, classIRI);
                else
                    for (GraphMap map : subjectMap.getGraphMaps()) 
                        if (map.getConstantValue() != null) 
                            dataset.add(subject, RDF.TYPE, classIRI, new URIImpl(map.getConstantValue().toString()));
    }

    /**
     * Process any Term Map
     *
     * @param map current term map
     * @param node current node in iteration
     * @return the resulting value
     */

    @Override
    public List<String> processTermMap(TermMap map, Object node) {
        List<String> value = new ArrayList<>();

        switch (map.getTermMapType()) {
            case REFERENCE_VALUED:
                //Get the expression and extract the value
                ReferenceIdentifierImpl identifier = (ReferenceIdentifierImpl) map.getReferenceValue();
                return extractValueFromNode(node, identifier.toString().trim());
            case CONSTANT_VALUED:
                //Extract the value directly from the mapping
                value.add(map.getConstantValue().stringValue().trim());
                return value;
                
            case FUNCTION_CALL_VALUED:
            	List<String> evaluatedparams = new ArrayList<String>();
    			for(ObjectMap om : map.getFunctionCall().getParameters()) {
    				List<Value> evaluatedparam = processObjectMap(om, node); //om.getValue(dbValues, dbTypes);
    				// TODO: many values, choose first one...
    				evaluatedparams.add(evaluatedparam.get(0).stringValue());
    			}			
    			
    			String result = null;
    			Object[] parameters = evaluatedparams.toArray(new String[] {});
    			try {
    				result = JSEnv.invoke(map.getFunctionCall().getFunctionName(), parameters);
    			} catch (NoSuchMethodException e) {
    				// TODO No error handling at this point, die!
    				System.err.println(e.getMessage());
    				System.exit(-1);
    			} catch (ScriptException e) {
    				// TODO No error handling at this point, die!
    				System.err.println(e.getMessage());
    				System.exit(-1);
    			}
    			
    			value.add(result);
    			return value;

            case TEMPLATE_VALUED:
                //Resolve the template
                String template = map.getStringTemplate();
                Set<String> tokens = R2RMLToolkit.extractColumnNamesFromStringTemplate(template);
                for (String expression : tokens) {
                    List<String> replacements = extractValueFromNode(node, expression);
                    for (int i = 0; i < replacements.size(); i++) {
                        if (value.size() < (i + 1)) {
                            value.add(template);
                        }
                        String replacement = null;
                        if(replacements.get(i) != null)
                            replacement = replacements.get(i).trim();

                        //if (replacement == null || replacement.isEmpty()) {
                        if (replacement == null || replacement.equals("")) {
                            //if the replacement value is null or empty, the reulting uri would be invalid, skip this.
                            //The placeholders remain which removes them in the end.
                            continue;
                        }

                        String temp = value.get(i).trim();
                        if (expression.contains("[")) {
                            expression = expression.replaceAll("\\[", "").replaceAll("\\]", "");
                            temp = temp.replaceAll("\\[", "").replaceAll("\\]", "");
                        }   
                        //JSONPath expression cause problems when replacing, remove the $ first
                        if ((map.getOwnTriplesMap().getLogicalSource().getReferenceFormulation() == QLTerm.JSONPATH_CLASS) 
                                && expression.contains("$")) {
                            expression = expression.replaceAll("\\$", "");
                            temp = temp.replaceAll("\\$", "");
                        }
                        try {
                            if (map.getTermType().toString().equals(TermType.IRI.toString())) {
                                //TODO: replace the following with URIbuilder
                                temp = temp.replaceAll("\\{" + Pattern.quote(expression) + "\\}",
                                        URLEncoder.encode(replacement, "UTF-8")
                                        .replaceAll("\\+", "%20")
                                        .replaceAll("\\%21", "!")
                                        .replaceAll("\\%27", "'")
                                        .replaceAll("\\%28", "(")
                                        .replaceAll("\\%29", ")")
                                        .replaceAll("\\%7E", "~"));
                            } else {
                                temp = temp.replaceAll("\\{" + expression + "\\}", replacement);
                            }
                        } catch (UnsupportedEncodingException ex) {
                            Logger.getLogger(AbstractRMLProcessor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        value.set(i, temp.toString());

                    }      
                }
                
                //Check if there are any placeholders left in the templates and remove uris that are not
                List<String> validValues = new ArrayList<>();
                for (String uri : value){
                    if (R2RMLToolkit.extractColumnNamesFromStringTemplate(uri).isEmpty()){
                        validValues.add(uri);
                    }
                }
                
                return validValues;

            default:
                return value;
        }

        //return value;
    }
    
    /**
     * Process a predicate object map
     *
     * @param dataset
     * @param subject   the subject from the triple
     * @param pom       the predicate object map
     * @param node      the current node
     */
    @Override
    public void processPredicateObjectMap(SesameDataSet dataset, Resource subject, PredicateObjectMap pom, Object node, TriplesMap map) {

        Set<PredicateMap> predicateMaps = pom.getPredicateMaps();
        //Go over each predicate map
        for (PredicateMap predicateMap : predicateMaps) {
            //Get the predicate
            List<URI> predicates = processPredicateMap(predicateMap, node);

            for (URI predicate : predicates) {
                //Process the joins first
                Set<ReferencingObjectMap> referencingObjectMaps = pom.getReferencingObjectMaps();
                for (ReferencingObjectMap referencingObjectMap : referencingObjectMaps) {
                    Set<JoinCondition> joinConditions = referencingObjectMap.getJoinConditions();

                    TriplesMap parentTriplesMap = referencingObjectMap.getParentTriplesMap();

                    //Create the processor based on the parent triples map to perform the join
                    RMLProcessorFactory factory = new ConcreteRMLProcessorFactory();
                    QLTerm queryLanguage = parentTriplesMap.getLogicalSource().getReferenceFormulation();

                    String source = parentTriplesMap.getLogicalSource().getIdentifier();

                    InputStream input = null;
                    try {
                        input = RMLEngine.getInputStream(source, parentTriplesMap);
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(AbstractRMLProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(AbstractRMLProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                                       
                    RMLProcessor processor = factory.create(queryLanguage);

                    RMLPerformer performer ;
                    //different Logical Source and no Conditions
                    if (joinConditions.isEmpty() & !parentTriplesMap.getLogicalSource().getIdentifier().equals(map.getLogicalSource().getIdentifier())) {
                        performer = new JoinRMLPerformer(processor, subject, predicate);
                        processor.execute(dataset, parentTriplesMap, performer, input);
                    } 
                    //same Logical Source and no Conditions
                    else if (joinConditions.isEmpty() & parentTriplesMap.getLogicalSource().getIdentifier().equals(map.getLogicalSource().getIdentifier())){
                        performer = new SimpleReferencePerformer(processor, subject, predicate);
                        if((parentTriplesMap.getLogicalSource().getReferenceFormulation().toString().equals("CSV")) || (parentTriplesMap.getLogicalSource().getReference().equals(map.getLogicalSource().getReference()))){
                            performer.perform(node, dataset, parentTriplesMap);
                        }
                        else{
                            int end = map.getLogicalSource().getReference().length();
                            log.info("RML:AbstractRMLProcessor " + parentTriplesMap.getLogicalSource().getReference().toString());
                            String expression = "";
                            switch (parentTriplesMap.getLogicalSource().getReferenceFormulation().toString()) {
                                case "XPath":
                                    expression = parentTriplesMap.getLogicalSource().getReference().toString().substring(end);
                                    break;
                                case "JSONPath":
                                    expression = parentTriplesMap.getLogicalSource().getReference().toString().substring(end+1);
                                    break;
                            }
                            processor.execute_node(dataset, expression, parentTriplesMap, performer, node, null);
                        }
                    }
                    //Conditions
                    else {
                        //Build a join map where
                        //  key: the parent expression
                        //  value: the value extracted from the child
                        HashMap<String, String> joinMap = new HashMap<>();

                        for (JoinCondition joinCondition : joinConditions) {
                            List<String> childValues = extractValueFromNode(node, joinCondition.getChild());

                            //Allow multiple values as child - fits with RML's definition of multiple Object Maps
                            for(String childValue : childValues){
                                joinMap.put(joinCondition.getParent(), childValue);  
                                if(joinMap.size() == joinConditions.size()){
                                    performer = new ConditionalJoinRMLPerformer(processor, joinMap, subject, predicate);
                                    processor.execute(dataset, parentTriplesMap, performer, input);
                                }
                            }
                        }
                    }

                }

                //process the objectmaps
                Set<ObjectMap> objectMaps = pom.getObjectMaps();
                for (ObjectMap objectMap : objectMaps) {
                    //Get the one or more objects returned by the object map
                    List<Value> objects = processObjectMap(objectMap, node);
                    for (Value object : objects) {
                        if (object.stringValue() != null) {
                            Set<GraphMap> graphs = pom.getGraphMaps();
                            if(graphs.isEmpty() && subject != null)
                                dataset.add(subject, predicate, object);
                            else
                                for (GraphMap graph : graphs) {
                                    Resource graphResource = new URIImpl(graph.getConstantValue().toString());
                                    dataset.add(subject, predicate, object, graphResource);
                                }
                                
                        }
                    }
                }
            }

        }
    }

    /**
     * process a predicate map
     *
     * @param predicateMap
     * @param node
     * @return the uri of the extracted predicate
     */
    private List<URI> processPredicateMap(PredicateMap predicateMap, Object node) {
        // Get the value
        List<String> values = processTermMap(predicateMap, node);

        List<URI> uris = new ArrayList<>();
        for (String value : values) {
            //TODO: add better control
            if(value.startsWith("www."))
                value = "http://" + value;
            uris.add(new URIImpl(value));
        }
        //return the uri
        return uris;
    }

    /**
     * process an object map
     *
     * @param objectMap
     * @param node
     * @return
     */
    public List<Value> processObjectMap(ObjectMap objectMap, Object node) {
        //A Term map returns one or more values (in case expression matches more)
        List<String> values = processTermMap(objectMap, node);

        List<Value> valueList = new ArrayList<>();
        for (String value : values) {
            switch (objectMap.getTermType()) {
                case IRI:
                    if (value != null && !value.equals("")){
                        if(value.startsWith("www."))
                            value = "http://" + value;
                        valueList.add(new URIImpl(value));}
                    break;
                case BLANK_NODE:
                    valueList.add(new BNodeImpl(value));
                    break;
                case LITERAL:
                    if (objectMap.getLanguageTag() != null && !value.equals("")) {
                        valueList.add(new LiteralImpl(value, objectMap.getLanguageTag()));
                    } else if (value != null && !value.equals("") && objectMap.getDataType() != null) {
                        valueList.add(new LiteralImpl(value, objectMap.getDataType()));
                    } else if (value != null && !value.equals("")) {
                        valueList.add(new LiteralImpl(value.trim()));
                    }
            }

        }
        return valueList;
    }
}
