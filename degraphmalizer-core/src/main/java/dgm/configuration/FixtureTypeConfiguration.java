package dgm.configuration;


import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Ernst Bunders
 */
public interface FixtureTypeConfiguration
{
    public JsonNode getMapping();
    public Iterable<JsonNode> getDocuments();
    public Iterable<String>getDocumentIds();
    public JsonNode getDocumentById(String id);
    public boolean hasDocuments();

}
