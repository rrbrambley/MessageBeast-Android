package com.alwaysallthetime.adnlibutils.db;

/**
 * AnnotationInstances is a MessageInstances in which all associated Messages have
 * an annotation with a common type.
 */
public class AnnotationInstances extends MessageInstances {
    private String mType;

    /**
     * Construct a new AnnotationInstances.
     *
     * @param type the Annotation type associated with this AnnotationInstances
     */
    public AnnotationInstances(String type) {
        super();
        mType = type;
    }

    /**
     * Get the Annotation type that all of the associated Messages have in common.
     *
     * @return the Annotation type that all of the associated Messages have in common.
     */
    public String getType() {
        return mType;
    }

    @Override
    public String getName() {
        return mType;
    }
}
