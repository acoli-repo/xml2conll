# Hands-On Example

## Installation
Simply clone this repository to a given location on your computer, we assume a unix-like operating system but it should work on windows with cygwin.

`git clone ...`

Open the cloned folder on your terminal.

## Usage

There are two entry points into our package, the Template-based conversion and the generic conversion, see [the main README](README.md) for more information. 

We provide a xml file and a template file you can work with in the example directory.
```
.
├── README.md
├── compile.sh
├── example
│   ├── example.xml
│   └── tutorial.json
├── lib/
├── run.sh
└── src/
```

### TemplateXMLConverter
Use the `run.sh` together with the class name to invoke the main function.

```shell script
./run.sh TemplateXMLConverter -f example/example.xml -t example/tutorial.json
```
If you do not wish to rely on templates, you may also invoke the template guessing routine with the `--guess` parameter.

```shell script
./run.sh TemplateXMLConverter -f example/example.xml --guess
```

For fine tuning you may change parameters like to file or sample size, see [the main README](README.md) for more information.

### GenericXMLConverter

Because evaluation of XPaths is rather expensive, we recommend using the generic converter for a faster conversion time.
We use this to quickly index a candidate file to check if it contains relevant linguistic annotation.

```shell script
./run.sh GenericXMLConverter -f example/example.xml
```