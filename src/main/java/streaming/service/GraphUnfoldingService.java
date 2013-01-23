package streaming.service;

import streaming.blueprints.GraphCommandListener;

/**
 * @author Ernst Bunders
 */
public interface GraphUnfoldingService
{
    public void unfoldVertex(String id, GraphCommandListener listener);
}

