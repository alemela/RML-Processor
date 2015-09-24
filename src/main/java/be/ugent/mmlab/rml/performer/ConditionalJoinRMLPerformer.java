package be.ugent.mmlab.rml.performer;

import be.ugent.mmlab.rml.core.RMLExecutionEngine;
import be.ugent.mmlab.rml.model.TriplesMap;
import be.ugent.mmlab.rml.processor.RMLProcessor;
import be.ugent.mmlab.rml.processor.termmap.TermMapProcessor;
import be.ugent.mmlab.rml.processor.termmap.TermMapProcessorFactory;
import be.ugent.mmlab.rml.processor.termmap.concrete.ConcreteTermMapFactory;
import be.ugent.mmlab.rml.sesame.RMLSesameDataSet;
import java.util.HashMap;
import java.util.List;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performer to do joins with rr:joinCondition
 *
 * @author mielvandersande, andimou
 */
public class ConditionalJoinRMLPerformer extends NodeRMLPerformer{
    private TermMapProcessor termMapProcessor ;
    
    // Log
    private static final Logger log = 
            LoggerFactory.getLogger(ConditionalJoinRMLPerformer.class);
    
    private HashMap<String, String> conditions;
    private Resource subject;
    private URI predicate;

    public ConditionalJoinRMLPerformer(
            RMLProcessor processor, HashMap<String, String> conditions, 
            Resource subject, URI predicate) {
        super(processor);
        this.conditions = conditions;
        this.subject = subject;
        this.predicate = predicate;
    }
    
    public ConditionalJoinRMLPerformer(
            RMLProcessor processor, Resource subject, URI predicate) {
        super(processor);
        this.subject = subject;
        this.predicate = predicate;
    }

    /**
     * Compare expressions from join to complete it
     * 
     * @param node current object in parent iteration
     * @param dataset
     * @param map 
     */
    @Override
    public void perform(Object node, RMLSesameDataSet dataset, 
        TriplesMap map, String[] exeTriplesMap, boolean pomExecution) {
        Value object;
        
        //iterate the conditions, execute the expressions and compare both values
        if(conditions != null){
            boolean flag = true;

            iter: for (String expr : conditions.keySet()) {
                log.debug("expr " + expr);
                String cond = conditions.get(expr);
                log.debug("cond " + cond);
                TermMapProcessorFactory factory = new ConcreteTermMapFactory();
                this.termMapProcessor = 
                factory.create(map.getLogicalSource().getReferenceFormulation());
                
                List<String> values = termMapProcessor.extractValueFromNode(node, expr);
                
                //TODO: check if it stops as soon as it finds something
                for(String value : values){
                    log.debug("value " + value);
                    if(value == null || !value.equals(cond)){
                            flag = false;
                            break iter;
                    }
                }
            }
            
            if(flag){
                object = processor.processSubjectMap(dataset, map.getSubjectMap(), node);
                if (subject != null && object != null){
                    dataset.add(subject, predicate, object);
                    log.debug("Subject " + subject 
                            + " Predicate " + predicate 
                            + " Object " + object.toString());
                    
                    if (exeTriplesMap != null) {
                        RMLExecutionEngine executionEngine =
                                new RMLExecutionEngine(exeTriplesMap);
                        pomExecution = executionEngine.
                                checkExecutionList(map, exeTriplesMap);
                    }
                    
                    if (!pomExecution) {
                        log.debug("Nested performer is called");
                        NestedRMLPerformer nestedPerformer =
                                new NestedRMLPerformer(processor);
                        nestedPerformer.perform(
                                node, dataset, map, exeTriplesMap, true);
                    }
                } 
                else
                    log.debug("Triple for Subject " + subject 
                            + " Predicate " + predicate 
                            + "Object " + object
                            + "was not created");
            }
        }       
    }
}