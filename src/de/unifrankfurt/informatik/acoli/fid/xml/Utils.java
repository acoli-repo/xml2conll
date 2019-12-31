package de.unifrankfurt.informatik.acoli.fid.xml;

import com.google.gson.Gson;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.logging.Logger;

public class Utils {
    private final static Logger LOGGER =
            Logger.getLogger(Utils.class.getName());
    static void printDocument(Document doc, OutputStream out) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc),
                    new StreamResult(new OutputStreamWriter(out, "UTF-8")));
        }
        catch (Exception e){
            System.err.println("Couldn't print");
        }
    }

    /**
     * reads the whole JSON template file and makes it iterable.
     */
    public static Template[] readJSONTemplates(String path) {
        LOGGER.fine("Loading templates from "+path);
        Template[] templates = {};
        Gson gson = new Gson();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            templates = gson.fromJson(in, Template[].class);
        }
        catch (FileNotFoundException e){
            // TODO: make some sort of default config, then change to warn.
            LOGGER.severe("Couldn't find templates file. Stacktrace: "+e.getStackTrace());
        }
        return templates;
    }
}
