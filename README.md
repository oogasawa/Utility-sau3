
# Utility-sau3

Utilities related to Docusaurus v3

## Installation

Tested environment:

* Ubuntu Linux 24.04
* JDK 23
* Apache Maven 3.9.9
* Utility-cli 3.1.0 [https://github.com/oogasawa/Utility-cli](https://github.com/oogasawa/Utility-cli)

For instructions on how to install JDK and Maven, refer for example to the following URL (installation is easier with SDKMAN!):
[https://sc.ddbj.nig.ac.jp/guides/software/DevelopmentEnvironment/java/](https://sc.ddbj.nig.ac.jp/guides/software/DevelopmentEnvironment/java/)

Since Utility-cli is not registered in the Maven Repository, you need to build it and install it in the local repository (`$HOME/.m2/`) of the machine where you will build this project.

```
git clone https://github.com/oogasawa/Utility-cli
cd Utility-cli
mvn clean install
```

## Build

```
git clone https://github.com/oogasawa/Utility-sau3
cd Utility-sau3
mvn clean package
```

This will create a fat-jar file (a single jar file containing all dependencies) named
`Utility-sau3/target/Utility-sau3-VERSION.jar`.

You only need this single fat-jar file to run the program. You can copy it to any suitable location if necessary.

## Usage

When you run it without any arguments, the usage information will be displayed.

``` bash
$ java -jar target/Utility-sau3-2.1.0.jar 

## Usage

java -jar Utility-sau3-VERSION.jar <command> <options>


## Docusaurus commands

sau:build       Build the Docusaurus project with filtered output.
sau:deploy      Build the Docusaurus project with filtered output and deploy it to the public_html directory.
sau:index       Making a full text index of multiple Docusaurus sites.
sau:start       Start the Docusaurus development server with filtered output and automatic port conflict handling.
sau:updateIndex Update a full text index of multiple Docusaurus sites.
sau:url         Change the URL of the Docusaurus site.


## Ex commands

ex:extract      Extract scripts from a markdown file.
ex:makefile     Generate a Makefile from a markdown file.


## Gemini commands

gemini:define   Return definition of a word or a phrase using Gemini AI
gemini:paraphrase   Rephrase English text into alternative expressions with similar meaning using Gemini AI
gemini:run      Simply run gemini with the given prompt.
gemini:toEnglish    Translate Japanese to English with gemini AI
gemini:toJapanese   Translate English to Japanese with gemini AI


## git commands

git:pull        Execute git pull on each subdirectory.
git:pushAll     Execute git push command on each subdirectory.
git:status      Execute git status command on each subdirectory.


## markdown commands

md:generate     Generate a docusaurus document template.
md:rename       Rename a docusaurus document.


## Other Commands

javadoc:buildAll    Build javadoc on all subdirectories.
javadoc:deploy  Build and deploy javadoc files.
sautest:generate    Saves the content of a SAVE-type block in markdown files.
sautest:run     Build and deploy javadoc files.

```

### markdown commands


#### md\:generate

When using Docusaurus to autogenerate the sidebar, a one-document-per-directory structure is created.
The template document will be generated in the current directory.

```bash
java -jar target/Utility-sau3-2.1.0.jar md:generate -id 010_Example_250810_oo01
```

The argument for the command-line option `-id` must follow this format:
“numeric order for the sidebar” + `_` + “document ID”

- `010_` specifies the numeric order in the sidebar when using autogeneration. This number is not displayed in the sidebar.
- Document ID

  - `Example` is the document name.
  - `250810` is the date (used to ensure uniqueness of the document ID).
  - `oo01` can be author initials or other identifiers (also used to ensure uniqueness of the document ID).

When executed, the following directory and file will be created:

```bash
$ tree 
.
└── 010_Example_250810_oo01
    └── 010_Example_250810_oo01.md

2 directories, 1 file
```

The content of the generated template file `010_Example_250810_oo01.md` will be:

```bash

id: Example_250810_oo01

```



#### md\:rename

This command renames an existing document in the current directory.
It changes the directory name, the file name, and the document ID at the beginning of the file.

```bash
java -jar target/Utility-sau3-2.1.0.jar md:rename -f 010_Example_250810_oo01 -t 050_Renamed_250810_oo01
```

Example execution:

```bash
$ java -jar ~/works/Utility-sau3/target/Utility-sau3-2.1.0.jar md:rename -f 010_Example_250810_oo01 -t 050_Renamed_250810_oo01
oogasawa@stonefly513:~/tmp (2025-08-18 12:16:07)
$ tree
.
└── 050_Renamed_250810_oo01
    └── 050_Renamed_250810_oo01.md

2 directories, 1 file
$ cat 050_Renamed_250810_oo01/050_Renamed_250810_oo01.md 

id: Renamed_250810_oo01

$
```

