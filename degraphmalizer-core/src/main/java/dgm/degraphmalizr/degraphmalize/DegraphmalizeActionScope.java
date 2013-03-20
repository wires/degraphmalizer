/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.degraphmalizr.degraphmalize;

/**
 * User: rico
 * Date: 14/03/2013
 */
public enum DegraphmalizeActionScope {
    INDEX
    {
        @Override
        public boolean isInScope(DegraphmalizeActionScope scope) {
            return INDEX.equals(scope);
        }
    },
    TYPE_IN_INDEX
    {
        @Override
        public boolean isInScope(DegraphmalizeActionScope scope)
        {
            return INDEX.equals(scope) || TYPE_IN_INDEX.equals(scope);
        }
    },
    DOCUMENT_ANY_VERSION
    {
        @Override
        public boolean isInScope(DegraphmalizeActionScope scope)
        {
            return INDEX.equals(scope) || TYPE_IN_INDEX.equals(scope) || DOCUMENT_ANY_VERSION.equals(scope);
        }
    },
    DOCUMENT {
        @Override
        public boolean isInScope(DegraphmalizeActionScope scope)
        {
            return INDEX.equals(scope) || TYPE_IN_INDEX.equals(scope) || DOCUMENT_ANY_VERSION.equals(scope) || DOCUMENT.equals(scope);
        }
    };

    public abstract boolean isInScope(DegraphmalizeActionScope scope);
}
