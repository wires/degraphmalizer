// ** disabled: depends on java 7 NEO2 features **
// Can be reinstated after we switch to java 7


//package configuration.javascript;
//
//import configuration.ConfigurationMonitor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.HashMap;
//import java.util.Map;
//
//import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
//import static java.nio.file.StandardWatchEventKinds.*;
//
///**
// * @author Ernst Bunders
// */
//public final class FileSystemCompositeWatcher
//{
//    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemCompositeWatcher.class);
//
//    @SuppressWarnings("unchecked")
//    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
//        return (WatchEvent<T>) event;
//    }
//
//    private final String configDir;
//    private boolean trace = false;
//
//    final protected ConfigurationMonitor configwatcher;
//
//    public FileSystemCompositeWatcher(String configDir, ConfigurationMonitor watcher)
//    {
//        this.configDir = configDir;
//        this.configwatcher = watcher;
//
//        try
//        {
//            new Thread(new DirectoryWatcher()).start();
//        }
//        catch (IOException e) {
//            LOGGER.error("Could not start the config file directory watcher on account of: " + e.getMessage(), e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    final class DirectoryWatcher implements Runnable {
//        private final WatchService watcher;
//        private final Map<WatchKey, Path> keys;
//
//
//        private DirectoryWatcher() throws IOException {
//            this.watcher = FileSystems.getDefault().newWatchService();
//            this.keys = new HashMap<WatchKey, Path>();
//            registerAll(Paths.get(configDir));
//            trace = true;
//        }
//
//        private void registerAll(final Path start) throws IOException {
//            // register directory and sub-directories
//            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
//                @Override
//                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
//                        throws IOException {
//                    register(dir);
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        }
//
//        private void register(Path dir) throws IOException {
//            final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
//            if (trace) {
//                Path prev = keys.get(key);
//                if (prev == null) {
//                    System.out.format("register: %s\n", dir);
//                } else {
//                    if (!dir.equals(prev)) {
//                        System.out.format("update: %s -> %s\n", prev, dir);
//                    }
//                }
//            }
//            keys.put(key, dir);
//        }
//
//        @Override
//        public void run() {
//            for (; ; ) {
//
//                // wait for key to be signalled
//                WatchKey key;
//                try {
//                    key = watcher.take();
//                } catch (InterruptedException x) {
//                    return;
//                }
//
//                final Path dir = keys.get(key);
//                if (dir == null) {
//                    System.err.println("WatchKey not recognized!!");
//                    continue;
//                }
//
//                for (final WatchEvent<?> event : key.pollEvents()) {
//                    final WatchEvent.Kind kind = event.kind();
//
//                    // TBD - provide example of how OVERFLOW event is handled
//                    if (kind == OVERFLOW) {
//                        continue;
//                    }
//
//                    // Context for directory entry event is the file name of entry
//                    final WatchEvent<Path> ev = cast(event);
//                    final Path name = ev.context();
//                    final Path child = dir.resolve(name);
//
//                    System.out.format("%s: %s\n", event.kind().name(), child);
//
//                    if (kind == ENTRY_CREATE && Files.isDirectory(child, NOFOLLOW_LINKS)) {
//                        try {
//                            //add this directory as to be watched.
//                            registerAll(child);
//                        } catch (IOException e) {
//                            LOGGER.error("Something went wrong reading the config directory: " + e.getMessage(), e);
//                            throw new RuntimeException(e);
//                        }
//                    } else {
//                        configwatcher.configurationChanged(index);
//                    }
//                }
//
//                // reset key and remove from set if directory no longer accessible
//                boolean valid = key.reset();
//                if (!valid) {
//                    keys.remove(key);
//
//                    // all directories are inaccessible
//                    if (keys.isEmpty()) {
//                        break;
//                    }
//                }
//            }
//        }
//    }
//}
