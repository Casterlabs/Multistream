package co.casterlabs.multistream.providers;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.function.Supplier;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.functional.tuples.Pair;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class RTMPProvider implements Supplier<Pair<OutputStream, Closeable>> {
    private String url;
    private FastLogger logger;

    public RTMPProvider(String url) {
        this.url = url;

        String name = this.url.substring(0, this.url.lastIndexOf('/'));
        this.logger = new FastLogger("RTMP Target: " + name);
    }

    @Override
    public Pair<OutputStream, Closeable> get() {
        try {
            Process ffmpeg = new ProcessBuilder()
                .command(
                    "ffmpeg",
                    "-hide_banner",
//                    "-v", "error",

                    "-i", "pipe:0",
                    "-c", "copy",
                    "-f", "flv",
                    this.url
                )
                .redirectInput(Redirect.PIPE)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.PIPE)
                .start();

            // Read the FFMPEG log and write it to our console using FastLoggingFramework.
            AsyncTask.create(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.logger.debug(line);
                    }
                } catch (IOException ignored) {}
            });

            return new Pair<>(ffmpeg.getOutputStream(), ffmpeg::destroy);
        } catch (IOException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            return null;
        }
    }

}
