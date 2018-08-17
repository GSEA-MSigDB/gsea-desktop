=======================
GSEA BINARY DISTRIBUTION
=======================

Prerequisites:

Java 8.0 (http://www.java.com).  Not compatible with Java 9+


Instructions:

1. Download and unzip the distribution file to a directory of your choice.

2. To start GSEA execute the following from the command line,

     java -Xmx4g -jar gsea.jar

Alternatively, you can start GSEA with one of the following scripts.  You 
might have to make the script executable (chmod a+x gsea.sh).


gsea.bat       (for Windows)
gsea.sh        (for Linux and macOS)
gsea.command   (for macOS, double-click to start)

The shell scripts are configured to start GSEA with 4GB of memory.  
This is a reasonable default for most machines.  If 
you are working with very large datasets you can increase the amount of 
memory available to GSEA by editing the first line of the startup script.
Specifically change the value of the "-Xmx" parameter.  For example,
to start GSEA with 8 GB of memory  change the value

   -Xmx4g

to

   -Xmx8g

