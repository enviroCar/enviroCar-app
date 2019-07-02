package org.envirocar.remote.serializer;


import org.envirocar.core.entity.PrivacyStatement;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class PrivacyStatementListDeserializer extends AbstractSimpleListSerde<PrivacyStatement> {

    /**
     * Constructor.
     *
     * @param rootKey     the root key in the json containing the list
     * @param entityClass the entity class to parse the json for
     */
    public PrivacyStatementListDeserializer(String rootKey, Class<PrivacyStatement> entityClass) {
        super(rootKey, entityClass);
    }
}
