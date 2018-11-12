#!/bin/bash
# Install a jar into the local maven repository. All parameters should be provided by the argument
/Users/gwu/ProgramFiles/apache-maven-3.1.1/bin/mvn install:install-file -Dfile=$1 -DgroupId=$2 -DartifactId=$3 -Dversion=$4 -Dpackaging=jar
