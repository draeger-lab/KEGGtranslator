# KEGGtranslator

<img align="right" src="https://github.com/draeger-lab/KEGGtranslator/blob/master/resources/de/zbit/kegg/gui/img/KEGGtranslatorIcon_64.png"/>

**A Java-based software for visualizing and translating the KEGG PATHWAY database. Conversion of KGML files into BioPAX, SBML, GraphML, GML, and various other formats.**

[![License (LGPL version 3)](https://img.shields.io/badge/license-LGPLv3.0-blue.svg?style=plastic)](http://opensource.org/licenses/LGPL-3.0)
[![Stable version](https://img.shields.io/badge/Stable_version-2.5-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/KEGGtranslator/releases/)
[![DOI](http://img.shields.io/badge/DOI-10.1093%20%2F%20bioinformatics%20%2F%20btr377-blue.svg?style=plastic)](http://dx.doi.org/10.1093/bioinformatics/btr377)

*Authors:* [Clemens Wrzodek](https://github.com/Clemens82/), [Finja Wrzodek](http://cogsys.cs.uni-tuebingen.de/mitarb/buechel/), [Andreas Zell](https://github.com/ZellTuebingen), Manuel Ruff, [Andreas Dräger](https://github.com/draeger/)

## Download

Latest versions of KEGGtranslator are available under [Releases](https://github.com/draeger-lab/KEGGtranslator/releases).

## Please cite

1. Clemens Wrzodek. Inference and integration of biochemical networks with multilayered omics data. PhD thesis, University of Tuebingen, Tübingen, Germany, June 2013. [ [link](http://www.dr.hut-verlag.de/978-3-8439-1116-0.html) ]
2. Clemens Wrzodek, Finja Büchel, Manuel Ruff, Andreas Dräger, and Andreas Zell. Precise generation of systems biology models from KEGG pathways. BMC Systems Biology, 7(1):15, January 2013. [ [DOI](http://dx.doi.org/10.1186/1752-0509-7-15) | [link](http://www.biomedcentral.com/1752-0509/7/15) | [pdf](http://www.biomedcentral.com/content/pdf/1752-0509-7-15.pdf) ]
3. Clemens Wrzodek, Andreas Dräger, and Andreas Zell. KEGGtranslator: visualizing and converting the KEGG PATHWAY database to various formats. Bioinformatics, 27(16):2314-2315, June 2011. [ [DOI](http://dx.doi.org/10.1093/bioinformatics/btr377) | [link](http://bioinformatics.oxfordjournals.org/content/27/16/2314) | [pdf](http://www.cogsys.cs.uni-tuebingen.de/mitarb/wrzodek/publications/2011-08-04-KEGGtranslator-with-color.pdf) ]

## KEGGtranslator options
The stand-alone tool provides a Graphical User Interface (GUI) mode and a command-line mode. All options (see section options) to customize the output (file format, and conversion options) are available through both modes. This allows users to use KEGGtranslator in a batch mode or with user interaction. The GUI can be started by passing the --gui option to the program. All other options passed to the program are adopted by the GUI.

### Usage
Start the graphical user interface with:
```
java -jar KEGGtranslator.jar
```

To use the command-line capabilities, please use the following syntax:
```
java -jar KEGGtranslator.jar --input [in_file.xml] --output [out_file] --format [out_format]
```

### Batch processing
KEGGtranslator can be used to batch convert multiple KGML formatted XML-files. To use this feature, simply give a folder as input argument, together with the desired output format and optional additional options. For example,
```
java -jar KEGGtranslator.jar --input C:/ --format SBML
```
will convert all KEGG files, found on drive C (including subdirectories) to SBML files.

### For developers: embedding KEGGtranslator
If you wan't to use KEGGtranslator inside your own application, you can simply put the JAR file on your class path and use the classes and methods of KEGGtranslator as described in the Javadoc (version 2.0, 1.1).

### Possible command-line arguments
#### 1. Basic KEGGtranslator IO Options
Define the default input/ output files and the default output format.
```
-i<File>, --input[ |=]<File>
```
Path and name of the source, KGML formatted, XML-file. Akzeptiert kGML-Dateien (*.xml).
```
-o<File>, --output[ |=]<File>
```
Path and name, where the translated file should be put.
```
-f<Format>, --format[ |=]<Format>
```
Target file format for the translation.
All possible values for type <Format> are: `SBML`, `SBML_L2V4`, `SBML_L3V1`, `SBML_QUAL`, `SBML_CORE_AND_QUAL`, `SBGN`, `BioPAX_level2`, `BioPAX_level3`, `SIF`, `GraphML`, `GML`, `JPG`, `GIF`, `TGF`, and `YGF`.
Default value: `SBML`

#### 2. KEGGtranslator Command Line Only Options
```
--cache-size[ |=]<Integer>
```
Specify the number of API entries from KEGG to keep into cache (default: `10000`).
Arguments must be in rage {[100,1000000]}.
Default: `10000`
```
--create-jpg
```
Create a visualization (as JPG) of the selected format. Always creates a JPG, even for SBML and others.
Default: `false`

##### KEGGtranslator Options
1. Generic translation options
Define various options that are used in all translations.
```
-ro, --remove-orphans
```
If true, remove all nodes that have no edges before translating the pathway.
Default: `false`
```
--gene-names[ |=]<NODE_NAMING>
```
For one KEGG object, multiple names are available. Choose how to assign one name to this object.
All possible values of `<NODE_NAMING>` are: `SHORTEST_NAME`, `FIRST_NAME_FROM_KGML`, `FIRST_NAME`, `ALL_FIRST_NAMES`, `INTELLIGENT_WITH_EC_NUMBERS`, and `INTELLIGENT`.
Default: `INTELLIGENT`
```
-formula, --show-formula-for-compounds
```
If true, shows the chemical formula for all compounds, instead of the name.
Default: `false`
```
-nowhite, --remove-white-gene-nodes
```
If `true`, removes all white gene-nodes in the KEGG document (usually enzymes that have no real instance on the current organism).
Default: `false`
```
-ar<Boolean>, --autocomplete-reactions[ |=]<Boolean>
```
If `true`, automatically looks for missing reactants and enzymes of reactions and adds them to the document.
```
-ar<Boolean>, --autocomplete-reactions[ |=]<Boolean>
```
Default: `true`
```
-nopr, --remove-pathway-references
```
If `true`, removes all entries (nodes, species, etc.) referring to other pathways.
Default: `false`

##### 2. Translation options for graphical outputs
Define various options that are used in yFiles based translations.
```
-merge, --merge-nodes-with-same-edges
```
If `true`, merges all nodes that have exactly the same relations (sources, targets and types).
Default: `false`
```
-cel, --create-edge-labels
```
If `true`, creates describing labels for each edge in the graph.
Default: `false`
```
-hc, --hide-labels-for-compounds
```
If `true`, hides labels for all compounds (=small molecules).
Default: `false`
```
-dar, --draw-grey-arrows-for-reactions
```
If `true`, creates grey arrows for reactions and arrows with transparent circles as heads for reaction modifiers. This does only affect reactions defined by KEGG, not the relations.
Default: `false`

#### 3. Translation options for SBML outputs
Define various options that are used in SBML based translations.
```
-layout, --add-layout-extension
```
If `true`, adds layout information, using the SBML layout extension to the SBML document.As a side-effect, this will create an SBML Level 3 model.
Default: `false`
```
-groups<Boolean>, --use-groups-extension[ |=]<Boolean>
```
If `true`, uses the SBML level 3 groups extension to encode groups in the SBML document.As a side-effect, this will create an SBML Level 3 model.
Default: `true`
```
-cbal<Boolean>, --check-atom-balance[ |=]<Boolean>
```
Check the atom balance of metabolic reactions and write a summary to the reaction notes. Depends on autocomplete reactions.
Default: `true`
Generic options for the graphical user interface
```
--check-for-updates[ |=]<Boolean>
```
Check for KEGGtranslator updates upon application start. 
Default: `true`
```
--gui
```
A graphical user interface is shown if this option is given.
Default: `false`
```
--log-level[ |=]<String>
```
Change the log-level of this application.
All possible values of `<String>` are: `ALL`, `CONFIG`, `FINE`, `FINER`, `FINEST`, `INFO`, `OFF`, `SEVERE`, and `WARNING`.
Default: `INFO`
