#!/usr/bin/env bash
# Used windows 7 Professional -- service pack 1
#
# 1. Setup java (jdk 1.8.144)
# ---------------------------
# Download jdk for windows x64 and install into default location C:/Program Files
#   http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
# Set the environment variable in System Variables
# JAVA_HOME to C:\Program Files\Java\jdk1.8.0_144
#
# 2. Setup maven (apache maven 3.5.0)
# -----------------------------------
# Download the zip and extract it to C:/Program Files
#   https://maven.apache.org/download.cgi
# Add C:/Program Files/apache-maven-3.5.0/bin to the path to enable the command mvn
#
# 3. Setup Inno Setup 5 (required by the jfx app bundler for the windows build)
# -----------------------------------------------------------------------------
# Download and install Inno Setup 5 or later from http://www.jrsoftware.org and add it to the PATH.
#  I installed innosetup-5.5.9.exe from here http://www.jrsoftware.org/isdl.php#stable and added it to the PATH.
#
# 4. Setup WiX 3.0 (required by the jfx app bundler for the windows build)
# ------------------------------------------------------------------------
# Download and install WiX 3.0 or later from http://wix.sf.net and add it to the PATH.
#  I installed v3.11 from here https://github.com/wixtoolset/wix3/releases/tag/wix311rtm and added it to the PATH.
#
# Please specify the name of the JNLP Outut file in 'jnlp.outfile'
#  I did not set this up because I did not need a 'WebStart JNLP Bundler'.
curl http://www.datavyu.org/docs/user-guide.pdf > packaged_docs/user-guide.pdf
# Add the -X flag to get debug information
# Removed jar:jar goal because package already includes that goal and it was run TWICE!
# See: https://stackoverflow.com/questions/40964500/maven-jar-plugin-3-0-2-error-you-have-to-use-a-classifier-to-attach-supplementa
#mvn -Prelease,win-package -Dmaven.test.skip=true clean package assembly:assembly jfx:native
mvn -Prelease,win-package -Dmaven.test.skip=true clean jfx:native
# The jfx:native will use the application and bundle it into installers (.exe and .msi)
# These installers are output to the folder ./target/jfx/native/
#
# If you want a zip you can add the following targets to the build command
#   package assembly:assembly
#