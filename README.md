#  Introduction

*Gene Set Enrichment Analysis* (GSEA) is a computational method that determines whether an a priori defined set of genes shows statistically significant, concordant differences between two biological states (e.g. phenotypes).  See the [GSEA website](http://www.gsea-msigdb.org) for more details.


GSEA Desktop is a free genomic analysis program written in the Java(tm) language implementing the GSEA method while providing preprocessing tools along with further analysis methods and visualizations.

# License

GSEA is made available under the terms of a BSD-style license, a copy of which is included in the distribution in the [LICENSE.txt](LICENSE.txt) file.  See that file for exact terms and conditions.

#  Latest Version
The latest binary release of this software can be obtained from the [Downloads page of the GSEA website](http://www.gsea-msigdb.org/gsea/downloads.jsp).

If you have any comments, suggestions or bugs to report, please see our [Contact page](http://www.gsea-msigdb.org/gsea/contact.jsp) for information on how to reach us.

# History and Acknowledgements

The **GSEA Desktop application version 1.0** was developed by Aravind Subramanian as part of his PhD thesis.  The work was supported by the Broad Institute of MIT and Harvard and advised by Jill Mesirov, Pablo Tamayo, Vamsi Mootha, Sayan Mukherjee, Todd Golub and Eric Lander.

Joshua Gould (code) and Heidi Kuehn (docs) contributed greatly to **GSEA Desktop 2.0**.  There were additional code contributions by Michael Angelo, Chet Birger, Justin Guinney, Keith Ohm, and Michael Reich.  

Thanks also to Vuk Pavlovic and Ruth Isserlin from the [Bader Lab at the University of Toronto](http://baderlab.org/) for their contribution of the Enrichment Map integration with Cytoscape.

**GSEA Desktop 3.0** is the open-source release.  

David Eby was responsible for the open-source conversion and handles current maintenance and new feature development. 
While David is listed on the initial commit to this public GitHub repository, original authorship is due to the 
individuals listed above regardless of the GitHub history metadata.

The initial GitHub commit roughly corresponds to the **GSEA Desktop version 3.0 Beta 2** release of October 13, 2016 with a few minor changes. The earlier code revision history is not available.

The GSEA project is currently a joint effort of the Broad Institute and the University of California San Diego, and funded by the National Cancer Institute of the National Institutes of Health (PI: JP Mesirov).

# Dependencies

GSEA Desktop is 100% Pure Java.  Java 17 is required for building and to run our pre-built binaries.  Builds against other versions of Java may be possible but are unsupported.

See the [LICENSE-3RD-PARTY.txt](LICENSE-3RD-PARTY.txt) file for a full list of the GSEA library dependencies.  In our binary builds, all required 3rd party library code is bundled along with the GSEA jar file so that no additional downloads or installation are required. 

------
Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
