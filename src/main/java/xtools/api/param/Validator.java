/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import edu.mit.broad.genome.Errors;

public class Validator {
    private final BooleanSupplier validationChecker;
    private final Supplier<Errors> errorMessageBuilder;
    public Validator(BooleanSupplier validationChecker, Supplier<Errors> errorMessageBuilder) {
        if (validationChecker == null || errorMessageBuilder == null) { throw new IllegalArgumentException("Validator args must not be null."); }
        this.validationChecker = validationChecker;
        this.errorMessageBuilder = errorMessageBuilder;
    }
    
    public boolean isValid() { return validationChecker.getAsBoolean(); }
    public Errors buildValidationFailedErrors() { return errorMessageBuilder.get(); }
}
