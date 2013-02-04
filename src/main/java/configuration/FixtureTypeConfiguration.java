package configuration;

import org.codehaus.jackson.JsonNode;

/**
 * @author Ernst Bunders
 */
public interface FixtureTypeConfiguration
{
    public JsonNode getMapping();
    public Iterable<JsonNode> getDocuments();
    public Iterable<String>getDocumentIds();
    JsonNode getDocumentById(String id);
}
