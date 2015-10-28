package org.envirocar.core.entity;

/**
 * @author dewall
 */
public class TermsOfUseImpl implements TermsOfUse {
    protected String id;
    protected String issuedDate;
    protected String contents;

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
    public TermsOfUse carbonCopy() {
        TermsOfUseImpl res = new TermsOfUseImpl();
        res.id = id;
        res.issuedDate = issuedDate;
        res.contents = contents;
        return res;
    }
}
