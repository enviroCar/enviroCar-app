package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface PrivacyStatement extends BaseEntity<PrivacyStatement> {
    String KEY_PRIVACY_STATEMENT = "privacyStatement";
    String KEY_PRIVACY_STATEMENT_ID = "id";
    String KEY_PRIVACY_STATEMENT_ISSUEDATE = "issuedDate";
    String KEY_PRIVACY_STATEMENT_CONTENTS = "contents";

    /**
     * Returns the id of the privacy statement.
     *
     * @return the id of the privacy statement.
     */
    String getId();

    /**
     * Sets the id
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * Returns the issued date of the privacy statement as string.
     *
     * @return the issued date of the privacy statement.
     */
    String getIssuedDate();

    /**
     * Sets the issued date of the privacy statement as string.
     *
     * @param issuedDate the issued date of the privacy statement.
     */
    void setIssuedDate(String issuedDate);

    /**
     * Returns the content of the privacy statement.
     *
     * @return the content of the privacy statement.
     */
    String getContents();

    /**
     * Sets the content of the  privacy statement as string.
     *
     * @param content the content of the privacy statement.
     */
    void setContents(String content);
}
