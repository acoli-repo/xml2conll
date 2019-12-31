package de.unifrankfurt.informatik.acoli.fid.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Logger;

public class XML2CoNLL {

    private XPathFactory XPATH = XPathFactory.newInstance();
    private SubtreeGenerator sg;
    private Template template;
    private final static Logger LOGGER =
            Logger.getLogger(XML2CoNLL.class.getName());

    /**
     * the SubtreeGenerator represents the file. The Template tells the converter
     * how to analyze the xml file to produce CoNLL.
     * @param sg
     * @param template
     */
    public XML2CoNLL(SubtreeGenerator sg, Template template){
        this.sg = sg;
        this.template = template;
    }

    /**
     *
     * @param out
     * @param n How many sentences to convert
     * @return
     */
    public void transform(PrintStream out, int n) {
        ArrayList<ArrayList<CoNLLRow>> conllSentences = new ArrayList<>();
        ArrayList<Document> xmlSentences = new ArrayList<>();
        try {
            xmlSentences = this.sg.getSamples(n);
        } catch ( XMLStreamException | IOException e) {
            e.printStackTrace();
        }

        for (Document xmlSentence : xmlSentences){
            try {
                ArrayList<CoNLLRow> words = consumeSentence(xmlSentence);
                conllSentences.add(words);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }

        LOGGER.info("Extracted "+conllSentences.size()+" sentences.");

            out.print(createCommentString(this.template)+"\n");
            for (ArrayList<CoNLLRow> sentence : conllSentences) {
                for (CoNLLRow word : sentence) {
                    out.print(word.toString()+"\n");
                }
                out.print("\n");
                out.flush();
            }
            out.close();

    }


    /**
     * expects one node representing a subtree and transforms it to CoNLL by using the TEMPLATE.
     * @param node
     * @return
     */
    public CoNLLRow transformToCoNLL(Node node, Integer i){
        CoNLLRow row = new CoNLLRow(i);
        if (this.template.columnPaths != null) {
            this.template.columnPaths.forEach((col, xpath) -> {
                try {
                    String result = XPATH.newXPath().compile(xpath).evaluate(node, XPathConstants.STRING).toString();
                    result = result == "" ? "_" : result;
                    row.getColumns().put(col, result);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        if (this.template.featurePaths != null) {
            this.template.featurePaths.forEach((feat, xpath) -> {
                try {
                    String result = XPATH.newXPath().compile(xpath).evaluate(node, XPathConstants.STRING).toString();
                    result = result.equals("") ? "_" : result;
                    LOGGER.finest(result);
                    row.getColumns().put(feat, result);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        LOGGER.finer("CONLL:"+row);
        return row;
    }

    /**
     * receives an entire subtree, split into words and chunks and calls functions to create
     * CoNLL rows out of them.
     * @param sentence
     * @return
     * @throws XPathExpressionException
     */
    public ArrayList<CoNLLRow> consumeSentence(Node sentence) throws XPathExpressionException{
        NodeList words = (NodeList) XPATH.newXPath().compile(this.template.wordPath).evaluate(sentence, XPathConstants.NODESET);
        ArrayList<CoNLLRow> sentenceRows = new ArrayList<>();
        LOGGER.fine("Handling a sentence with "+words.getLength()+" words.");
        for (int i = 0; i < words.getLength(); i++){
            sentenceRows.add(transformToCoNLL(words.item(i), i));
        }
        return sentenceRows;
    }

    /**
     * creates the comment string denoting column names based on a template.
     * @param template
     * @return
     */
    public String createCommentString(Template template){
        ArrayList<String> comment = new ArrayList<>();
        for (String column : template.columns){
            comment.add(column);
        }
        // in case we have features we replace the comment section.
        if (template.feats != null) {
            ArrayList<String> feats = new ArrayList<>();
            feats.addAll(template.feats);

            if (comment.contains("FEATS")){ // replaces the keyword with the actual featurenames
                comment.set(comment.indexOf("FEATS"), String.join("|",feats));
            }
            else{
                comment.add(String.join("|", feats));
            }
        }
        // result
        return "#"+String.join("\t", comment);
    }
}
