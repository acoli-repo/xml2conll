# XML2CoNLL

This java package can be used to transform xml to conll. We focus on linguistic corpora data, however you may have success extending the template approach to other fields.

## Usage

We provide two ways of interfacing with the `xml2conll` package. First a command line interface, second a java API to use in your projects.
Both are explained in the following seconds. [Here](TEMPLATES.md) we explain how to define templates for a given xml file.
See [our hands-on tutorial](EXAMPLE.md) for a quick example of the command line interface.


## Java API

### `TemplateXMLConverter` 

In case you want to use the Template converter, you will need a template json file to go with it, that the converter
will try to match to.
The result will be written to the PrintStream object. In case you want to write to a file directly, you may simply use the utility function
`convertFileToPrintStream(File)`.

```java
TemplateXMLConverter txc = new TemplateXMLConverter("Path/To/Template/File");
File xmlFile = ...
File targetFile = ...

txc.getFullCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile));
```
This will test all given templates together with a sample of the file in order to find the template that represents
the file's structure best. See the paper for further details on how we calculate the quality of a template.
 
In case you previously know which Template matches best, you can also provide it directly:


```java
Template template = ...
txc.getFullCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile), template);
```

### Template guessing

In order to quickly create new templates to integrate into a conversion pipeline, we provide a template guesser
that creates a conversion template for a given unknown xml file based on a few heuristics. 
```java
TemplateXMLConverter txc = ...
File unknownXMLFile = ...
Template guessedTemplate = TemplateGuesser.guessTemplate(unknownXMLFile);
File targetFile = ...
txc.getFullCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile), guessedTemplate);
```


### `GenericXMLConverter`
The second way of using our package is through the Generic converter. Note that the result is not proper conll,
as it does not have sentence borders and potentially not a word per line. However, we transform all xml attributes
to a tab separated file that most likely is able to be processed by other CoNLL compatible pipelines. 

We use this to quickly sample from a XML file to see if there are any linguistic annotations present. If yes,
we perform the slower but more precise template conversion.

Again, use the Util function to convert the File to a PrintStream.
```java
GenericXMLConverter txc = new GenericXMLConverter();
File xmlFile = ...
File targetFile = ...
txc.getFullPseudoCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile));
```

### Commandline interface

In case you prefer calling our package from the command line, we also provide a commandline interface.
All required .sh scripts are provided with this repository. 

### TemplateXMLConverter
* Converts known xml dialects into conll, based on templates.
* templates are contained in lib/templates.json and can be added manually
* `run.sh TemplateXMLConverter -f IN_FILE -t TEMPLATE_PATH [-o OUT_FILE] [-l LENGTH] [-s SAMPLE_SIZE] [--silent] [--guess]`
  * IN_FILE: XML file to convert
  * TEMPLATE_PATH: path to template json
  * OUT_FILE: default std out, where to write converted conll
  * LENGTH: how many sentences to convert, will fully convert if empty
  * SAMPLE_SIZE: default 10, How many sentences to sample
  * --guess: will ignore the templates and guess one instead
  * --silent: no logging output (also not this synopsis!)

* E.g.: `./run.sh TemplateXMLConverter -f tinytest.xml -t lib/templates.json`



### GenericXMLConverter
* `GenericXMLRecognizer -f FILE [-n LENGTH] [--silent]`
  * FILE: XML file to check
  * LENGTH: how many sentences to convert, will fully convert if empty
  * --silent: no logging output (also not this synopsis!)
* E.g.: `./run.sh GenericXMLConverter -f tinytest.xml`


