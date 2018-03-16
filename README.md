# Datavyu

[Datavyu](http://datavyu.org/) is an open-source research tool that integrates and displays all kinds of data, letting you discover the big picture while remaining connected with raw data. Datavyu will let you build and organize interpretations, and will assist with analysis.

## Download and Use Datavyu
You can find binaries of Datavyu available for Windows and OSX on [the Datavyu.org download page](http://datavyu.org/download/).

## Development Requirements

To get started with Datavyu development, you will need to download and install a few development tools. Datavyu is primarily written in Java, along with a little Ruby (via JRuby) for additional scripting tasks. So the list of toys you will need to download:

* [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
* [Maven 3.0.5](http://maven.apache.org/)
* [Git](http://git-scm.com/)

## Datavyu OSX And Windows builds

To build and package Datavyu, use the following commands:

	git clone https://github.com/databrary/datavyu.git
	cd datavyu
	export MAVEN_OPTS="-Xmx256M"
	mvn clean -U -Dmaven.test.skip=true jfx:native

## Build and Running Datavyu as Javafx Application through Maven in Intellij

Follow these steps for the setup.

1. Next to the run button click on the drop-down menu item 'Edit configuration'
2. In the dialog select on the left-hand side 'maven' and then '+'
3. Fill out working directory: 'C:/Users/Florian/integration/datavyu' (yours is different)
4. Add the command line: -Dmaven.test.skip=true clean compile jfx:run (for now we exclude tests)

Note, this setup will not run with the debugger in Intellij.


## Debugging Datavyu as Java Application in Intellij

Follow these steps for the setup.

1. Next to the run button click on the drop-down menu item 'Edit configuration'
2. In the dialog select on the left-hand side select 'Defaults' and 'Application' then '+'
3. Fill out the working directory 'C:/Users/Florian/integration/datavyu' (yours is different)
4. Add as main class 'org.datavyu.Datavyu'


## Developer's Corner
Datavyu uses the Swing Application Framework to persist the state of frames and windows and to ease the loading and setup of GUI components in java. 
Further information about the framework can be found here: https://en.wikipedia.org/wiki/Swing_Application_Framework#cite_note-1.
However, this framework is not officially supported by Sun (Java) as the proposal was withdrawn over design disputes: https://jcp.org/en/jsr/results?id=3801.
Documentation on using this framework is limited and one of the best places to find documentation is here: http://www.oracle.com/technetwork/articles/javase/index-141957.html#code19.
Also, notice that underneath the Swing Application Framework uses JavaBeans to provide additional storage; aside from the internal mechanism of saving state of the windows in properties files. This mechanism uses the LocalStorage class; see Example 19 (http://www.oracle.com/technetwork/articles/javase/index-141957.html#code19). 
A tutorial on javaBeans is here: http://docs.oracle.com/javaee/5/tutorial/doc/bnair.html.

## Deployment Information
pre_version.txt, version.txt

## More Information

See the [wiki](https://github.com/databrary/datavyu/wiki) for more information on how to code and contribute improvements to Datavyu.

A list of features and fixes that need implementing for Datavyu can be found [here](http://datavyu.org/bugs).

