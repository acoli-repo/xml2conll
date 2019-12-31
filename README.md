# XML2CoNLL

This java package can be used to transform xml to conll. We focus on linguistic corpora data, however you may have success extending the template approach to other fields.

## Usage

This package has two main entry points via commandline interface.

### XML to CoNLL Conversion, based on templates
* Converts known xml dialects into conll, based on templates.
* templates are contained in lib/templates.json and can be added manually
* Usage: `./run.sh XMLConverter -f IN_FILE -t TEMPLATE_PATH [-o OUT_FILE] [-l LENGTH] [-s SAMPLE_SIZE] [--silent]`
  * IN_FILE: XML file to convert
  * TEMPLATE_PATH: path to template json
  * OUT_FILE: default std out, where to write converted conll
  * LENGTH: default 999, how many sentences to convert
  * SAMPLE_SIZE: default 500, How many nodes to sample
  * --silent: no logging output (also not this synopsis!)
* Sample usage: `./run.sh XMLConverter -f tinytest.xml -t lib/templates.json`



### Generic XML Recognition
* Finds potential linguistic annotation within xml files. provide an xml file
* Usage: `./run.sh GenericXMLRecognizer -f FILE [-s SAMPLE_SIZE] [-t THRESHOLD] [--silent]`
  * FILE: XML file to check
  * SAMPLE_SIZE:  default 500, How many nodes to sample
  * THRESHOLD: default 0.6, float what percentage of checks should pass
  * --silent: no logging output (also not this synopsis!)
* Sample usage: `./run.sh GenericXMLRecognizer -f tinytest.xml`


