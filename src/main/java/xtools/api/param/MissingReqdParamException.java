/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

/**
 * @author Aravind Subramanian
 */
public class MissingReqdParamException extends RuntimeException {

    private Param[] missingParams;

    private String msgInHtml;

    /**
     * Class constructor
     *
     * @param missingParams
     */
    public MissingReqdParamException(final Param[] missingParams) {
        super("\n\nSome required parameters (" + missingParams.length + ") were not specified. The parameters are:\n" + _str(missingParams));
        this.missingParams = missingParams;
    }

    private static String _str(Param[] params) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            buf.append(">").append(params[i].getName()).append("<").append('\t').append(params[i].getDesc()).append('\n');
        }

        return buf.toString();
    }

    public String getMessageLongInHtml() {

        if (msgInHtml != null) {
            return msgInHtml;
        }

        StringBuffer buf = new StringBuffer("<html>\n" +
                "<body>\n" +
                "<p>Required parameter(s) were not specified:</p>\n" +
                "<ul>\n");

        for (int i = 0; i < missingParams.length; i++) {
            buf.append("<li> ").append(missingParams[i].getNameEnglish()).append("</li>\n");
        }

        buf.append("</ul>\n").append(
                "<br><br>Please set these in the form and try again").append("</body>\n").append("</html>");

        return buf.toString();
    }

} // End class MissingReqdParameterException
