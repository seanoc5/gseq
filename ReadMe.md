# Groovy Solr Extractor of Queries (gseq)

## Overview
This is a simple groovy project to help extract solr queries to (CSV) files to enable load testing based on actual usage. There are likely easier/simpler ways to do this with sed/awk/perl, but I like the versatility and adaptability of groovy/java and related classpath enhancements.

## Setup
I like sdkman (https://sdkman.io/) to manage JVM versions and the many related tools (like gradle and groovy).

Ensure you have the following stack installed/available:
- JVM 8+
- Gradle 5+ _(possibly lower?)_
- Groovy 3.x+ _(possibly lower?)_

SDKMan is a helpful tool for installing various versions of jdks, gradle, groovy, and many other java-related tools:
- https://sdkman.io/
- curl -s "https://get.sdkman.io" | bash

Look for the user-adjustable settings in the script _(TODO's highlight where those should become setable with CLI args)_

## Todo
- implement command line option parsing
- finish moving the script to a more complete class/tool
- flesh out gradle tasks
- write useful tests 
