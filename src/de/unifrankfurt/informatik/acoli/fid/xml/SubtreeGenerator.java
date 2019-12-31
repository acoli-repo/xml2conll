package de.unifrankfurt.informatik.acoli.fid.xml;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * SubtreeGenerator should be used to read in any .xml file that represents a corpus. It splits up a xml file of
 * arbitrary size into subtrees with a previously specified name. (e.g.: each sentence subtree).
 * Evaluates the xml lazily, thus saving significantly on memory. Also hold's a few service functions for
 * optional use.
 * @author lglaser
 *
 */
public class SubtreeGenerator implements Iterable<Document>, Iterator<Document>{

	private final static Logger LOGGER =
			Logger.getLogger(SubtreeGenerator.class.getName());
	private String sentenceName;
	private String filePath;
	private File file;
	private XMLEventReader xmlReader;
	private Integer sentenceCounter; // TODO: change naming to make sure this is the current INDEX
	private Integer documentLength;
	private FileReader fileReader;


	public SubtreeGenerator(String sentenceName, String filePath) throws FileNotFoundException{
		LOGGER.info("Instantiating subtreeGenerator with String: "+filePath);
		this.sentenceName = sentenceName;
		File inFile;
		try {
			URL fileURL = new URL(filePath);
			inFile = FileUtils.toFile(fileURL);
		} catch (MalformedURLException e1) {
			LOGGER.info("MalformedURL, trying as local filePath..");
			inFile = new File(filePath);
		}
		this.file = inFile;
		this.filePath = filePath;
		this.fileReader = new FileReader(this.file);
		try {
			this.initialize(this.file);
		} catch (XMLStreamException e) {
			LOGGER.severe("Unable to initialize the reader. Stacktrace:");
			e.printStackTrace();
		}
	}


	public SubtreeGenerator(String sentenceName, File file) throws FileNotFoundException {
		LOGGER.info("Instantiating subtreeGenerator with File: "+file.getAbsolutePath());
		this.sentenceName = sentenceName;
//		this.filePath = file.getPath();
		this.file = file;
		this.filePath = this.file.getPath();
		this.fileReader = new FileReader(this.file);
		try{
			this.initialize();
		}
		catch (FileNotFoundException e){
			System.err.println("Couldn't find file: "+filePath);
		} catch (XMLStreamException e) {
			System.err.println("Unable to initialize the reader. Stacktrace:");
			e.printStackTrace();
		}
	}

	/**
	 * Sets up the XMLEventReader and sets it as an object variable.
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	private void initialize() throws XMLStreamException, FileNotFoundException {
		Reader xml = new FileReader(this.file);
		XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
		XMLEventReader  staxReader   = staxFactory.createXMLEventReader( xml );
		setXmlReader(staxReader);
		setSentenceCounter(0);
	}
	private void initializeIGNORE() throws FileNotFoundException, XMLStreamException {
		FileReader fileReader = new FileReader(this.file);
		XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
		XMLEventReader staxReader   = staxFactory.createXMLEventReader( fileReader );
		setXmlReader(staxReader);
		setSentenceCounter(0);
	}
	/**
	 * Initializes the XMLReader directly with a file.
	 * @param file
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	private void initialize(File file) throws XMLStreamException, FileNotFoundException{
		Reader xml = new FileReader(file);
		XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
		staxFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		staxFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		staxFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		staxFactory.setXMLResolver(new XMLResolver() {
			@Override
			public Object resolveEntity(String arg0, String arg1, String arg2,
										String arg3) throws XMLStreamException {
				throw new XMLStreamException("Reading external entities is disabled");
			}
		});
		XMLEventReader  staxReader   = staxFactory.createXMLEventReader( xml );
		setXmlReader(staxReader);
		setSentenceCounter(0);
	}

	/**
	 * Initializes the XMLReader directly with a given xmlReader
	 * @param xml
	 * @throws XMLStreamException
	 */
	private void initialize(BufferedReader xml) throws XMLStreamException{
		XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
		XMLEventReader  staxReader   = staxFactory.createXMLEventReader( xml );
		setXmlReader(staxReader);
		setSentenceCounter(0);
	}

	/**
	 * Initializes again, renamed to have things more readable and provide public access
	 * without BufferedReader argument
	 *
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public void reset(){
		try {
			this.initialize(); // maybe don't just call init but do it properly?
		} catch (XMLStreamException e) {
			System.err.println("Couldn't reset the SubtreeGenerator");
			e.printStackTrace();
		}catch (FileNotFoundException e) {
			System.err.println("File went missing.");
			e.printStackTrace();
		}
	}

	private void incrementSentenceID(){
		Integer counter = getSentenceCounter();
		counter++;
		setSentenceCounter(counter);
	}


	//========================================================================
	// TREE NAVIGATION AND SAMPLING
	//========================================================================
	/**
	 *
	 * @return
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public Integer getDocumentLength() throws XMLStreamException, FileNotFoundException {
		// if we computed it already, we just get it.
		if (this.documentLength != null) {
			return this.documentLength;
		}
		// otherwise we compute the documentLength and save it for later use.
		else {
			Integer length = -1;
			try {
				this.reset();
				while (this.hasNext()){
					skip();
					length++;
				}
			} catch (Exception e) {
				setDocumentLength(length);
				System.err.println("XML Document is broken, ending parsing.");
				System.err.println(e.getMessage());
				throw e;
//				return this.documentLength;
			}
			finally {
				this.reset(); // set the generator back to avoid side effects
				System.out.println("Computed document length of "+length+" for file "+this.getFilePath()+".");
				setDocumentLength(length);
				return this.documentLength;
			}
		}
	}
	void initialCalculateDocumentLength() {

	}
	public void setDocumentLength(Integer documentLength) {
		this.documentLength = documentLength;
	}
	/**
	 * TODO: document
	 * @param k
	 * @return
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public ArrayList<Document> getSamples(Integer k) throws XMLStreamException, IOException{
//		System.err.println(k);
		this.reset();
		int doclen = this.getDocumentLength();
		if (k > doclen) {
			System.out.println("sample size ("+k+") exceedes number of sentences, reducing to corpus size ("+this.getDocumentLength()+").");
			k = doclen;
		}
		ArrayList<Document> samples = new ArrayList<>();
		try {
			ArrayList<Integer> sampleIndices = createSampleIndices(k);
			Collections.sort(sampleIndices);
			for (Integer sampleIndex : sampleIndices) {
				skipUntil(sampleIndex);
				Document next = next();
				samples.add(next);
			}
		} catch (IllegalArgumentException e){
			return samples;
		}
		return samples;
	}

	/**
	 * creates k random indices originating from the number of sentences in one document.
	 * @param k
	 * @return
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	ArrayList<Integer> createSampleIndices(Integer k) throws IOException, XMLStreamException{
		ArrayList<Integer> result = new ArrayList<>();
		Integer maxIndex = this.getDocumentLength()+1;
		// TODO: somehow ints 1, max Index breaks this, change to 0 to have it work, valve plz fix
		result = ThreadLocalRandom.current().ints(1, maxIndex).distinct().limit(k).boxed().collect(Collectors.toCollection(ArrayList::new));
		return result;
	}

	/**
	 * continues the XMLReader until the subtree which next() would return has
	 * finished. Saves the overhead of reading, parsing and returning the subtree.
	 * @throws XMLStreamException
	 */
	public void skip() throws XMLStreamException {
		if (this.hasNext()){
			XMLEventReader current = getXmlReader();
			XMLEvent nextElement = null;
			try{
				int errorcount = 0; // TODO: remove this, hacky way to get around DTD annotations.
				while (current.hasNext()){
					try {
						nextElement = current.nextEvent();
						} catch (com.ctc.wstx.exc.WstxParsingException e){
						errorcount++;
						if (errorcount>3) {
							throw e;
						}
						continue;
					}
					if (isSubtreeEnd(nextElement) || nextElement.getEventType()==XMLEvent.END_DOCUMENT){
//						current.next(); TODO CHECK IF nextElement PEEKS!!
						setXmlReader(current);
						return;
					}
					}


			}
			catch (XMLStreamException e){
				System.err.println("Unable to skip at subtree "+getSentenceCounter()+", ignoring remaining subtrees.");
				System.err.println(e.getMessage());
				e.printStackTrace();
					throw e;
			}
			finally{
				incrementSentenceID();
			}
		}
	}
	/**
	 * wraps skip() and calls it k times.
	 * @param k
	 * @throws XMLStreamException
	 */
	public void skip(Integer k) throws XMLStreamException{
		for (int i = 0; i<k; i++){
			skip();
		}
	}

	/**
	 * wraps skip() and calls it until subtree at index i is reached
	 * TODO: was passiert wenn i > korpuslänge?
	 * @param i
	 * @throws XMLStreamException
	 */
	public void skipUntil(Integer i) throws XMLStreamException{
		while (getSentenceCounter()<i){
			skip();
		}
	}

	/**
	 * prettier check if an Event opens up a new subtree.
	 * @param xmlEvent
	 * @return
	 */
	public boolean isSubtreeBegin (XMLEvent xmlEvent){
		return (xmlEvent.isStartElement() && this.sentenceName.equals(xmlEvent.asStartElement().getName().toString()));
	}

	/**
	 * prettier check if an Event closes the current subtree.
	 * @param xmlEvent
	 * @return
	 */
	public boolean isSubtreeEnd (XMLEvent xmlEvent){
		return (xmlEvent.isEndElement() && this.sentenceName.equals(xmlEvent.asEndElement().getName().toString()));
	}

	//========================================================================
	// SUBTREE PARSING
	//========================================================================
	/**
	 * receives the xmlReader in a state which (should) begin at a subtree. Then runs until
	 * the ending of the subtree and returns it.
	 * TODO: it's unclear to why peek and next are called, fix this and make consistent with calling
	 * iterator.
	 * @param xmlReader
	 * @return
	 * @throws XMLStreamException
	 * @throws ParserConfigurationException
	 */
	public Document collectSubtree(XMLEventReader xmlReader){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document subtree = null;
		try {
			db = dbf.newDocumentBuilder();
			subtree = db.newDocument();

			// Using an XMLWriter that writes directly to a DOM Object
			XMLEventWriter writer;
			writer = XMLOutputFactory.newInstance().createXMLEventWriter(new DOMResult(subtree));
			while (xmlReader.hasNext()){
				XMLEvent nextEvent = xmlReader.nextEvent();
				if (isSubtreeEnd(nextEvent) || nextEvent.getEventType() == XMLEvent.END_DOCUMENT){ // if we see an end element we're done
//					xmlReader.next();
					break;
				}
				writer.add(nextEvent); // otherwise we continue adding events
//				xmlReader.next();
			}
		}catch (XMLStreamException | ParserConfigurationException e ) {
			System.err.println("Unable to collect the subtree with index "+getSentenceCounter());
			e.printStackTrace();
		}
		return subtree;
	}

	@Override
	public boolean hasNext() {
		return (getXmlReader().hasNext());
	}

	/**
	 * Generates the next subtree of the specified xml document. Will continue to read until the subtree closes 
	 * again.
	 */
	public Document next() {
		Document result = null;
		if (this.hasNext()){
			XMLEventReader current = getXmlReader(); // get the current state of the reader
			XMLEvent nextElement = null;
			try {
				while(current.hasNext()){
					nextElement = current.peek();
					if (nextElement.getEventType() == XMLEvent.END_DOCUMENT) {
						return result;
					}
					if (isSubtreeBegin(nextElement)){
						Document subtree = collectSubtree(current);
						current.next();
						setXmlReader(current); // saving back the state
						return subtree;
					}
					current.next(); // continuing the reader object
				}
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				incrementSentenceID();
			}
		} // TODO: This could be moved to finally statement probably?
		if (result == null){
			System.err.println("Broken document");
		}
		return result;
	}




	//========================================================================
	// ITERATOR, SETTERS AND GETTERS
	//========================================================================

	@Override
	public Iterator<Document> iterator() {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}
	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * in case the sentenceCounter does not exist yet, we compute it first.
	 * @return the sentenceCounter
	 */
	public Integer getSentenceCounter() {
		return sentenceCounter;
	}

	/**
	 * @param sentenceCounter the sentenceCounter to set
	 */
	public void setSentenceCounter(Integer sentenceCounter) {
		this.sentenceCounter = sentenceCounter;
	}

	/**
	 * @return the xmlReader
	 */
	XMLEventReader getXmlReader() {
		return xmlReader;
	}

	/**
	 * @param xmlReader the xmlReader to set
	 */
	void setXmlReader(XMLEventReader xmlReader) {
		this.xmlReader = xmlReader;
	}
}
