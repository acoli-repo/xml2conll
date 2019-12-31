package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * class containing functions to recognize .xml files that contain some sort of linguistic annotations.
 * There are two hyper parameters to set:
 * <ul>
 *     <li>ALL_CHECKS_THRESHOLD: What percentage of checks must pass to accept a given annotation as relevant?</li>
 *     <li>SINGLE_CHECK_THRESHOLD: What percentage of annotations must pass a single check for the check to be true?</li>
 * </ul>
 */
public class GenericXMLRecognizer {
    static private float ALL_CHECKS_THRESHOLD = 0.6f; // TODO: where to parametrize this?
    static private float SINGLE_CHECK_THRESHOLD = 0.8f;
    static private int SAMPLE_SIZE = 500;
    static private String SYNOPSIS = "synopsis: GenericXMLRecognizer -f FILE [-s SAMPLE_SIZE] [-t THRESHOLD] [--silent]\n"+
            "\tFILE          XML file to check\n"+
            "\tSAMPLE_SIZE   default 500, How many nodes to sample\n"+
            "\tTHRESHOLD     default 0.6, float what percentage of checks should pass\n"+
            "\t--silent      no logging output (also not this synopsis!)\n";

    private final static Logger LOGGER =
            Logger.getLogger(GenericXMLRecognizer.class.getName());
    // TODO: make simple one-sentence conll
    // TODO: also it would be nice to have arbitrary check number, but I have no clue about reflection, maybe later.

    /**
     * receives an XML File and cuts a sample of size size. Each item i1,...,in is one
     * XMLNode.
     * @param xmlFile a xml file that is to be sampled
     * @param n number of nodes to sample
     * @return a DOM sample from the xml file
     */
    static private Document cutSample(File xmlFile, Integer n) {
        Document sample = null;
        try {
            // Setup IO
            FileReader fileReader = new FileReader(xmlFile);
            XMLInputFactory staxFactory = XMLInputFactory.newInstance();
            XMLEventReader staxReader = staxFactory.createXMLEventReader(fileReader);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            sample = db.newDocument();

            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(new DOMResult(sample));

            for (int i = 0; i < n; i++) {
                if (staxReader.hasNext()) {
                    writer.add((XMLEvent) staxReader.next());
                }
                else {
                    LOGGER.info("Node count for sample ("+n+") too small, entire file has "+i+" nodes.");
                    break;
                }
            }
        } catch (XMLStreamException | FileNotFoundException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return sample;
    }

    /**
     * TODO: collect CDATA as well
     * @param document a DOM tree
     * @return a HashMap of Path-to-Attribute -> value -> number of occurrences of value
     * @throws XMLStreamException if malformed xml
     */
    private static HashMap<String, ArrayList<String>> collectOverview (Document document) throws XMLStreamException {
        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new DOMSource(document));
        HashMap<String, ArrayList<String>> overview = new HashMap<>();
        while (reader.hasNext()){
            XMLEvent nextEvent = (XMLEvent) reader.next();
            if (nextEvent.isStartElement()){
                Iterator items = nextEvent.asStartElement().getAttributes();
                while (items.hasNext()){
                    Attribute att = (Attribute) items.next();

                    String nameSpace = nextEvent.asStartElement().getName().getNamespaceURI();
                    String attName = nextEvent.asStartElement().getName().toString();
                    if (nameSpace.length()>1) { // removes namespace annotation, maybe we might as
                        // well keep it?
                        attName = attName.replace("{" + nameSpace + "}", "");
                    }
                    String path = attName+"-"+att.getName();
                    ArrayList<String> temp = overview.getOrDefault(path, new ArrayList<>());
                    temp.add(att.getValue());
                    overview.put(path, temp);
                }
            }
        }
        return overview;
    }

    static public boolean hasNode (File xmlFile, String node, int n) {
        try {
            FileReader fileReader = new FileReader(xmlFile);

        XMLInputFactory staxFactory = XMLInputFactory.newInstance();
        XMLEventReader staxReader = staxFactory.createXMLEventReader(fileReader);

        int i = 0;
        while (i < n && staxReader.hasNext()){
            XMLEvent nextEvent = (XMLEvent) staxReader.next();
            if (nextEvent.isStartElement() && nextEvent.asStartElement().getName().toString().equals(node)){
                return true;
            }
        }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e){
            e.printStackTrace();
        }
        return false; // we didn't find any or experienced an exception
    }
    static public HashMap<String, HashMap<String, Integer>> findPossibleAttributes(File xmlFile) throws XMLStreamException {
        HashMap<String, ArrayList<String>> overview = collectOverview(cutSample(xmlFile, SAMPLE_SIZE));
        HashMap<String, HashMap<String,Integer>> counts = new HashMap<>();

        // create a count table from collected data
        // this roughly corresponds to conll columns
        for (String key : overview.keySet()){
            HashMap<String, Integer> count = new HashMap<>();
            for (String value : overview.get(key)){
                count.put(value,count.getOrDefault(value, 0)+1);
            }
            counts.put(key, count);
        }

        HashMap<String, HashMap<String, Integer>> result = new HashMap<>();
        for (String attribute : counts.keySet()){
            HashMap<String, Integer> overviewOfAttribute = counts.get(attribute);
            float maxHitCount = 3; // TODO: if new method, ++ this.
            float hitCount = 0;
            // execute checks:
            if (hasSmallDomain(overviewOfAttribute)){
                hitCount++;
            }
            if (hasUppercaseDomain(overviewOfAttribute)){
                hitCount++;
            }
            if (domainHasNoSpaces(overviewOfAttribute)){
                hitCount++;
            }

            // add to result, if candidate for linguistic annotation
            if (hitCount/maxHitCount>ALL_CHECKS_THRESHOLD){
                result.put(attribute, overviewOfAttribute);
            }
        }
        return result;
    }

    // CHECK FUNCTIONS counts -> boolean

    /**
     * Check if all values of attributes have a limited vocabulary. This is most likely the case for
     * linguistic annotations.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    public static boolean hasSmallDomain(HashMap<String, Integer> counts){
        float factor = 0.4f;
        double tokens = counts.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
        //System.err.println(sum+" "+sum*factor);
        double type = counts.size();
        LOGGER.info("type: "+type+", tokens: "+tokens+", ttr: "+type/tokens+", factor: "+factor+" ttr<factor: "+(type/tokens < factor));
        return type/tokens < factor;
    }

    /**
     * Check if most attributes are in caps. This is likely for linguistic annotations like postags, however
     * dependency relations often are not.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    private static boolean hasUppercaseDomain(HashMap<String, Integer> counts) {
        long upperCaseCount = counts.keySet()
                .stream()
                .filter(e -> !e.equals(e.toLowerCase()))
                .count();
        return upperCaseCount >= counts.size() * SINGLE_CHECK_THRESHOLD;
    }

    /**
     * Check if most attributes do not contain spaces. This is most likely the case for linguistic annotations.
     * @param counts annotation -> how often it occurred in an attribute
     * @return if this check is true
     */
    private static boolean domainHasNoSpaces(HashMap<String, Integer> counts){
        long noSpaceCount = counts.keySet()
                .stream()
                .filter(e -> !e.contains(" "))
                .count();
        return noSpaceCount >= counts.size() * SINGLE_CHECK_THRESHOLD;
    }

    /**
     * Main method to access all functionality from cmd line.
     * @param args arguments
     */
    public static void main(String[] args){
        String filePath = "";
        HashMap<String, HashMap<String, Integer>> result;
        for ( int i = 0; i < args.length; i++ ) {
            switch (args[i]) {
                case "-f":
                    i++;
                    filePath = args[i];
                    break;
                case "-s":
                    i++;
                    SAMPLE_SIZE = Integer.parseInt(args[i]);
                    break;
                case "-t":
                    i++;
                    ALL_CHECKS_THRESHOLD = Float.parseFloat(args[i]);
                    break;
                case "--silent":
                    LOGGER.setLevel(Level.OFF);
                    break;
                default:
                    break;
            }
        }

        if (filePath.length() == 0){
            LOGGER.severe("NO FILE PROVIDED.");
            LOGGER.info(SYNOPSIS);
            System.exit(1);
        }
        try {
            LOGGER.info(SYNOPSIS);
            LOGGER.info("FILE: "+filePath+"\n"+
                    "SAMPLE_SIZE: "+SAMPLE_SIZE+"\n"+
                    "THRESHOLD: "+ALL_CHECKS_THRESHOLD);
            result = findPossibleAttributes(new File(filePath));
            for (Map.Entry<String, HashMap<String, Integer>> entry : result.entrySet()){
                System.out.println(entry.getKey()+": "+entry.getValue());
            }
        } catch (XMLStreamException e){
            System.err.println("Couldn't parse file at "+filePath);
        }
    }
}
