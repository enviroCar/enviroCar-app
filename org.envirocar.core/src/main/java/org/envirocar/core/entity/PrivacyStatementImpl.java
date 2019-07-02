package org.envirocar.core.entity;

/**
 * @author dewall
 */
public class PrivacyStatementImpl implements PrivacyStatement {
    protected String id;
    protected String issuedDate;
    protected String contents;

    public PrivacyStatementImpl(){
    }

    public PrivacyStatementImpl(String id, String issuedDate, String contents) {
        this.id = id;
        this.issuedDate = issuedDate;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    @Override
    public PrivacyStatement carbonCopy() {
        PrivacyStatementImpl res = new PrivacyStatementImpl();
        res.id = id;
        res.issuedDate = issuedDate;
        res.contents = contents;
        return res;
    }
}
