package co.casterlabs.multistream;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;

// Modified from: https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
@AllArgsConstructor
public abstract class FileWatcher extends Thread {
    private File file;

    public abstract void onChange();

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = this.file.getCanonicalFile().getParentFile().toPath();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watcher.poll(25, TimeUnit.MILLISECONDS);

                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    WatchEvent.Kind<?> kind = event.kind();
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                        && filename.toString().equals(this.file.getName())) {
                            this.onChange();
                        }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
