package de.unifrankfurt.informatik.acoli.fid.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Template {

	private String id;
	public String getId() {
		return id;
	}

	public String description;
	public String chunkPath;
	public String sentencePath;
	public String wordPath;
	public LinkedHashMap<String, String> columnPaths;
	public LinkedHashMap<String, String> featurePaths;
	public ArrayList<String> columns;
	public ArrayList<String> feats;
	
	Template(){ // empty constructor for gson serialization
	}
	
	/**
	 * throws together all values from the extraction template and returns
	 * them.
	 * @return
	 */
	HashSet<String> getAllPaths(){
    	HashSet<String> paths = new HashSet<>(this.columnPaths.values());
    	if (this.featurePaths != null) {
			paths.addAll(this.featurePaths.values());
		}
    	return paths;
	}

	public String getSentencePath(){
		return this.sentencePath;
	}
	
	@Override
	public String toString() {
		return "XMLTemplate #"+this.id+"\n\tDescription: '"+this.description+"'\n";
	}
	@Override
	public boolean equals(Object obj) {
		try {
			final Template other = (Template) obj;
			if (this.id.equals(other.id)) {
				return true;
			}
			else {
				return false;
			}
		}	
		catch (ClassCastException e) {
			return false;
		}
	}
}
