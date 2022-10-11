package co.casterlabs.multistream.providers;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.function.Supplier;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.commons.platform.Platform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileProvider implements Supplier<Pair<OutputStream, Closeable>> {
    private static final File RECORD_DIR = new File("./recordings");

    public static final Supplier<Pair<OutputStream, Closeable>> INSTANCE = new FileProvider();

    static {
        RECORD_DIR.mkdirs();
    }

    @Override
    public Pair<OutputStream, Closeable> get() {
        try {
            File dest = new File(RECORD_DIR, String.format("Recording - %d.mkv", System.currentTimeMillis()));

            Process containerizer = new ProcessBuilder()
                .command(
                    "ffmpeg",
                    "-hide_banner",
                    "-v", "error",

                    "-i", "pipe:0",
                    "-c", "copy",

                    dest.getCanonicalPath()
                )
                .redirectInput(Redirect.PIPE)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)
                .start();

            FastLogger.logStatic(LogLevel.DEBUG, "Recording started: %s", dest);

            return new Pair<>(
                containerizer.getOutputStream(),
                () -> {
                    // Attempt to gracefully kill FFMPEG, allowing it to write out the final
                    // headers.
                    switch (Platform.osFamily) {
                        case UNIX: {
                            long PID = containerizer.pid();
                            Runtime.getRuntime().exec("kill -2 " + PID);
                            break;
                        }

                        case WINDOWS: {
                            long PID = containerizer.pid();
                            Runtime.getRuntime().exec("taskkill /pid " + PID);
                            break;
                        }

                        default: {
                            FastLogger.logStatic(LogLevel.WARNING, "Unable to gracefully kill FFMPEG, the recorded file will not be seekable.");
                            containerizer.destroy();
                            return;
                        }
                    }

                    AsyncTask.create(() -> {
                        try {
                            Thread.sleep(250); // Give FFMPEG 250ms to terminate.

                            if (containerizer.isAlive()) {
                                FastLogger.logStatic(LogLevel.WARNING, "Had to forcefully kill FFMPEG, the recorded file will not be seekable.");
                                containerizer.destroy();
                            }

                            FastLogger.logStatic(LogLevel.DEBUG, "Recording finished: %s", dest);
                        } catch (InterruptedException e) {}
                    });
                }
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
