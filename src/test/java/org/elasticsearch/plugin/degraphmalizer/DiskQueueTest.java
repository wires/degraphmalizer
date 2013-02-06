/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * User: rico
 * Date: 04/02/2013
 */
@Test
public class DiskQueueTest {

    @Test
    public void testQueueReadWrite() {
        DiskQueue testQueue = new DiskQueue("/tmp/testqueue");
        Change change1 = new Change(Action.UPDATE, "poms", "urn:vpro:media:program:1906", 100);
        Change change2 = new Change(Action.DELETE, "poms", "urn:vpro:media:program:1906", 100);
        Change change3 = new Change(Action.UPDATE, "poms", "urn:vpro:media:program:190336", 9);
        Change change4 = new Change(Action.DELETE, "poms", "urn:vpro:media:program:123906", 33);
        Change change5 = new Change(Action.UPDATE, "poms", "urn:vpro:media:program:19206", 10);
        List<Change> changes = new ArrayList();
        changes.add(change1);
        changes.add(change2);
        changes.add(change3);
        changes.add(change4);
        changes.add(change5);
        try {
            testQueue.writeToDisk(changes);
        } catch (IOException e) {
            assertThat(true, equalTo(false));
        }
        List<Change> changesFromDisk;
        try {
            changesFromDisk = testQueue.readFromDisk();
        } catch (IOException e) {
            assertThat(true, equalTo(false));
            changesFromDisk=null;
        }
        assertThat(changesFromDisk,notNullValue());
        assertThat(changesFromDisk, hasSize(5));
        assertThat(change1,equalTo(changesFromDisk.get(0)));
    }
}
