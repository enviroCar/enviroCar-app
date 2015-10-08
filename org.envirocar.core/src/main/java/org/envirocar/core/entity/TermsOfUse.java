package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TermsOfUse extends BaseEntity {
    String KEY_TERMSOFUSE = "termsOfUse";
    String KEY_TERMSOFUSE_ID = "id";
    String KEY_TERMSOFUSE_ISSUEDDATE = "issuedDate";

    /**
     * Returns the id of the terms of use.
     *
     * @return the id of the terms of use.
     */
    String getId();

    void setId(String id);

    /**
     * Returns the issued date of the terms of use as string.
     *
     * @return the issued date of the terms of use.
     */
    String getIssuedDate();

    void setIssuedDate(String issuedDate);

    /**
     * Returns the content of the terms of use.
     *
     * @return the content of the terms of use.
     */
    String getContents();

    void setContents(String content);
}
