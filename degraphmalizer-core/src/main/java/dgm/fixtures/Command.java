/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

/**
 * User: rico
 * Date: 08/04/2013
 */
public interface Command<V>
{
    public V execute() throws Exception;
}
