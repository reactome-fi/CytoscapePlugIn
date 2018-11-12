#!/bin/bash
# Install a jar into the local maven repository. All parameters should be provided by the argument
/Users/wug/ProgramFiles/apache-maven-3.5.0/bin/mvn install:install-file -Dfile=$1 -DgroupId=$2 -DartifactId=$3 -Dversion=$4 -Dpackaging=jar
