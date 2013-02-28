//package configuration.javascript;
//
//import com.google.inject.Provider;
//import configuration.ConfigurationMonitor;
//import configuration.IndexConfig;
//import org.junit.*;
//
//import java.io.File;
//import java.io.IOException;
//
//import static org.junit.Assert.assertEquals;
//
///**
// * What to test:
// * - when a directory is created, no new config should be created.
// * - in any other case, a new config should be created.
// * - when a new directory is create, it should be watched too
// *
// * @author Ernst Bunders
// */
//public final class FileSystemConfigWatchableTest {
//
//    private static final String ROOT_DIR = "watchTest";
//    private File rootDir;
//    private FileSystemCompositeWatcher fscw;
//    private final CountingConfigBuilder configBuilder = new CountingConfigBuilder();
//
//    @Before
//    public void createFiles() {
//        rootDir = createDir(new File("/tmp"), ROOT_DIR);
//        fscw = new FileSystemCompositeWatcher(rootDir.getAbsolutePath(), configBuilder);
//    }
//
//    @After
//    public void removeFiles() throws IOException, InterruptedException {
//        deleteDir(rootDir);
//        configBuilder.reset();
//    }
//
//    @Test
//    public void testNoConfigReloadOnNewDir() throws InterruptedException {
//        createDir(rootDir, "d1");
//        pauze();
//        assertEquals("Creating empty dirs should not cause config reloading", 0, configBuilder.counter);
//    }
//
//    @Test
//    public void testAddFileTriggersConfigReload() throws IOException, InterruptedException {
//        createFile(rootDir, "f1");
//        createFile(rootDir, "f2");
//        pauze();
//        assertEquals("Adding files should cause config reloading", 2, configBuilder.counter);
//    }
//
//    @Test
//    public void testAddFileInNewDirTriggersConfigReload() throws IOException, InterruptedException {
//        File d1 = createDir(rootDir, "d1");
//        skip();
//        createFile(d1, "f1");
//        pauze();
//        assertEquals("Adding a dir and than a file in that dir should cause config reloading", 1, configBuilder.counter);
//    }
//
//    @Test
//    public void testDeletingFileTriggersConfigReload() throws IOException, InterruptedException {
//        File f = createFile(rootDir, "f1");
//        skip();
//        configBuilder.reset();
//        f.delete();
//        pauze();
//        assertEquals("Deleting files should cause config reloading", 1, configBuilder.counter);
//
//    }
//
//    @Test
//    public void testDeletingDirTriggersConfigReload() throws InterruptedException {
//        File d1 = createDir(rootDir, "d1");
//        Thread.sleep(10);
//        d1.delete();
//        Thread.sleep(500);
//        assertEquals("Deleting dirs should cause config reloading", 1, configBuilder.counter);
//    }
//
//    @Test
//    public void testChangingFilesShouldTriggerConfigReload() throws IOException, InterruptedException {
//        File f1 = createFile(rootDir, "f1");
//        skip();
//        configBuilder.reset();
//        //now we can change the file.
//        f1.setLastModified(System.currentTimeMillis());
//        pauze();
//        //for some reason changing a file this way creates two events.
//        assertEquals("Changing a file should cause config reloading", 1, configBuilder.counter);
//    }
//
//    @Test
//    public void testObserversAreNotifiedOnConfigReload() throws IOException, InterruptedException {
//        CountingConfigNotifier watcher = new CountingConfigNotifier();
//        fscw.addConfigWatcher(watcher);
//        createFile(rootDir, "f1");
//        pauze();
//        assertEquals("Observer should be notified", 1, watcher.counter);
//    }
//
//
//
//
//    /*
//    The event system is a bit slow. We have to wait for it a little. This is
//    not a problem in practice, When files are updated manually
//     */
//    private void pauze() throws InterruptedException { Thread.sleep(500); }
//    private void skip() throws InterruptedException { Thread.sleep(10); }
//
//
//    private void deleteDir(File dir) throws IOException, InterruptedException {
//        Process p = Runtime.getRuntime().exec("rm -rf /tmp/" + ROOT_DIR);
//        p.waitFor();
//    }
//
//    private File createDir(File dir, String name) {
//        final File newDir = new File(dir, name);
//        newDir.mkdir();
//        return newDir;
//    }
//
//    private File createFile(File dir, String name) throws IOException {
//        final File newFile = new File(dir, name);
//        newFile.createNewFile();
//        return newFile;
//    }
//
//    private static final class CountingConfigBuilder implements Provider<IndexConfig>
//    {
//        int counter = 0;
//
//        @Override
//        public IndexConfig get() {
//            counter++;
//            return null;
//        }
//
//        public void reset() {
//            counter = 0;
//        }
//
//    }
//
//    private static final class CountingConfigNotifier implements ConfigurationMonitor
//    {
//        int counter = 0;
//
//        @Override
//        public void configurationChanged(IndexConfig indexConfig) {
//            counter ++;
//        }
//
//        public void reset(){counter = 0;}
//    }
//}