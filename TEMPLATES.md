# Template Tutorial

The conversion templates are used to define XPath queries that are called during conversion from XML to CoNLL.
Throughout this document, we will refer to this XML snippet as our input:


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
	<s id="s1">
		<w id="s1w1" lem="hello">Hello</w>
		<w id="s1w2" lem="world">World</w>
		<w id="s1w3">!</w>
	</s>
	<s id="s2">
		<w id="s2w1" lem="this">This</w>
		<w id="s2w2" lem="work">works</w>
		<chunk id="c1">
			<w id="s2w3" lem="like">like</w>
			<w id="s2w4" lem="a">a</w>
			<w id="s2w5" lem="breeze">breeze</w>
			<w id="s2w6">.</w>
		</chunk>
	</s>
</root>
```

A template can be easily defined in JSON. This would be an example of a template that converts the example above into
conll:
```json
{	
	"sentencePath" : "s",
	"wordPath" : "//w",
	"columnPaths" : {
		"CDATA": "text()",
		"lemma": "@lem"
	}
}
```
Let's go step by step through the definition process.

First, we need to define the sentencePath in the xml document that corresponds to the sentence node. In the example case,
this is the `s` node. Thus:
```json
{
    "sentencePath" : "s"
}
```
Next, we define where the node that represents a word is located. In the example, this is the `w` node. Thus:
```json
{
    "sentencePath" : "s",
    "wordPath" : "//w"
}
```
We use the double slashes so that the query returns all w nodes in all depths. That way, both w nodes in and out of the chunk span
are collected for further processing. 

Now, we can extract the data we are interested in from the XML. We do this as well using xPaths. There are two important commands:
`@ATT` returns the value of the attribute called `ATT`, `NODE/text()` returns the CDATA of the `NODE` node. 
In this step, we are no longer concerned with splitting up the document. 

Next, we define where to find the data to put into the CoNLL field. 
Each column in a CoNLL document represents a specific annotation to the word on that line. We do this in the `columnPaths` attribute
of our template. It consists simply of key-value of the column's name and the corresponding XPath. E.g. to extract the
CDATA of each word node, the key-value pair would be: `"CDATA" : "text()"`. For the lemma attribute you would input `"lemma" : "@lem"`.
Remember, the key name can be anything you want, this is merely used for the leading comment line in the resulting CoNLL file.

The final template then looks like this:
```json
{	
	"sentencePath" : "s",
	"wordPath" : "//w",
	"columnPaths" : {
		"CDATA": "text()",
		"lemma": "@lem"
	}
}
```
And the result of the conversion is this:

```
0	Hello	hello	
1	World	world	
2	!	_	

0	This	this	
1	works	work	
2	like	like	
3	a	a	
4	breeze	breeze	
5	.	_	
```

## Advanced Usage

For more complicated documents, you can use more complex xPaths queries to get what you want.
In our document, we omitted the id attributes. They all have the same name, but are located in different locations
of the document, so we would expect them to be different annotations.

The word id is easy, similar to the lemma: `"wordID" : "@id"`.
The sentence requires to search from the root of the sentence subtree again. XPath does that with a `//`. Therefore we use: `"sentID" : "//s/@id"`
The chunk id is hardest to get. We need to *climb up* the tree again using `..`. From there we can *go down* the tree again. Thus we can use:
`"chunkID": "../../chunk/@id` to get the chunk id. Thus results in the following CoNLL:

```text
0	Hello	hello	s1w1	_	s1	
1	World	world	s1w2	_	s1	
2	!	_	s1w3	_	s1	

0	This	this	s2w1	_	s2	
1	works	work	s2w2	_	s2	
2	like	like	s2w3	c1	s2	
3	a	a	s2w4	c1	s2	
4	breeze	breeze	s2w5	c1	s2	
5	.	_	s2w6	c1	s2	
```
