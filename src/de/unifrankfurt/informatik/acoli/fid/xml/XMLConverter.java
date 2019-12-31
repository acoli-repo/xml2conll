package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XMLConverter {
    private final static Logger LOGGER =
            Logger.getLogger(XMLConverter.class.getName());
    static private String SYNOPSIS = "synopsis: XMLConverter -f IN_FILE -t TEMPLATE_PATH [-o OUT_FILE] [-l LENGTH] [-s SAMPLE_SIZE] [--silent]\n"+
            "\tIN_FILE       XML file to convert\n"+
            "\tTEMPLATE_PATH path to template json\n"+
            "\tOUT_FILE      default std out, where to write converted conll\n"+
            "\tLENGTH        default 999, how many sentences to convert\n"+
            "\tSAMPLE_SIZE   default 500, How many nodes to sample\n"+
            "\t--silent      no logging output (also not this synopsis!)\n";


    public XMLConverter(){

    }
    /**
     * Receives an xml file, and the name of the sentence nodes. Will then load
     * all templates and convert.
     * @param sg
     * @param out
     * @param n
     * @param template
     */
    public void parse(SubtreeGenerator sg, PrintStream out, int n, Template template) {

        XML2CoNLL xmlParser = new XML2CoNLL(sg, template);
        xmlParser.transform(out, n);
        // Figure out a fitting one
    }


    public static void main(String[] args) {
        String filePath = null;
        String templatePath = null;
        String outPath = null;
        int n = 999;
        int k = 10;
        // First, read in cmd line args
        for (int i = 0; i<args.length; i++) {
            switch (args[i]) {
                case "-f":
                    i++;
                    filePath = args[i];
                    break;
                case "-t":
                    i++;
                    templatePath = args[i];
                    break;
                case "-o":
                    i++;
                    outPath = args[i];
                    break;
                case "-l":
                    i++;
                    n = Integer.parseInt(args[i]);
                    break;
                case "-s":
                    i++;
                    k = Integer.parseInt(args[i]);
                    break;
                case "--silent":
                    LOGGER.setLevel(Level.OFF);
                    break;
                default:
                    break;
            }
        }

        LOGGER.info(SYNOPSIS);
        if (filePath == null){
            LOGGER.severe("NO FILE PROVIDED.");
            System.exit(1);
        }
        File xmlFile = new File(filePath);
        // first, load the templates from disk
        ArrayList<Template> templates = new ArrayList<>(Arrays.asList(Utils.readJSONTemplates(templatePath)));
        // get all possible sentence boarders
        HashSet<String> sentenceNameCandidates = templates.stream()
                .map(Template::getSentencePath)
                .collect(Collectors.toCollection(HashSet::new));

        String sentenceName = null;
        for (String sentenceNameCandidate : sentenceNameCandidates){
            if (GenericXMLRecognizer.hasNode(xmlFile, sentenceNameCandidate, k)) {
                sentenceName = sentenceNameCandidate;
            }
        }
        if (sentenceName == null){
            LOGGER.warning("No Template found with fitting sentenceName for file "+filePath);
        }

        try {
            SubtreeGenerator sg = new SubtreeGenerator(sentenceName, xmlFile);
            ArrayList<Document> sample = sg.getSamples(k);

            TemplateMatcher tm = new TemplateMatcher(templates);
            TemplateQuality bestMatch = tm.getBestTemplateQuality(sample);

            PrintStream out = outPath == null ? System.out : new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outPath))));

            XMLConverter xc = new XMLConverter();
            xc.parse(sg, out, n, bestMatch.getTemplate());
        } catch (XMLStreamException e ) {
            LOGGER.severe("Couldn't parse XML file at"+filePath);
            e.printStackTrace();
            System.exit(1);
        } catch (FileNotFoundException e) {
            LOGGER.severe(filePath + "doesn't exist.");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
