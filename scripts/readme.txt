=======================
GSEA Binary Distribution
=======================

Prerequisites:

Java 17 or 21 (http://openjdk.java.net).  Java 21 is bundled with our distributions.
It is likely to also work on Java 22 and above, though this is untested.  It is not
compatible with earlier Java versions.


Instructions:

1. Download and unzip the distribution file to a directory of your choice.

2. You can start GSEA with one of the following scripts; this is the recommended 
way to launch.  Some of these may not be present depending on the distribution you
downloaded.  You might have to make the script executable (chmod a+x gsea.sh).


gsea.bat       (for Windows)
gsea.sh        (for Linux and macOS)
gsea_hidpi.sh  (for Linux with HiDPI screens)
gsea-cli.sh    (for Linux and macOS command line usage)
gsea-cli.bat   (for Windows command line usage)
gsea.command   (for macOS, double-click to start)

These scripts are configured to start GSEA with 4GB of memory.  This is a 
reasonable default for most machines.  If you are working with very large datasets 
you can override this setting (and other Java-related defaults) by editing GSEA's
java_arguments file, found here (create it if it doesn't exist):
   $HOME/.gsea/java_arguments           (Mac and Linux)
   %USERPROFILE%/.gsea/java_arguments   (Windows)

Specifically set the value of the "-Xmx" parameter.  For example, to start GSEA with 
8 GB of memory add the following to the file: 

-Xmx8g

This will override the default 4GB memory specification.

Other Java-related command-line options can also be set in this file, though changing anything
beyond the memory specification is for advanced users only and is not recommended.  See
   https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html
for more information on the Java 21 command line, and
   https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#java-command-line-argument-files
in particular for specifics of the "java_arguments" file format.

Use gsea-cli.sh (Linux, Mac) or gsea-cli.bat (Windows) to run GSEA at the command line,
For example:
     $ gsea-cli.sh GSEA [parameters]
Or, more generally
     $ gsea-cli.sh [operationName] [parameters]
     
Where [operationName] is one of GSEA, GSEAPreranked, CollapseDataset, Chip2Chip, or LeadingEdgeTool
for the chosen operation and [parameters] are the corresponding parameters for that operation.
Use of the Command feature in the UI is the best way to discover the available parameters 
for each operation.
