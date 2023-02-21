package co.casterlabs.multistream.providers;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Supplier;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.functional.tuples.Pair;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class FFMPEGProvider implements Supplier<Pair<OutputStream, Closeable>> {
    private String command;
    private FastLogger logger;

    public FFMPEGProvider(String command) {
        this.logger = new FastLogger("FFMPEG Target: " + command.hashCode());
        this.command = command;
    }

    @Override
    public Pair<OutputStream, Closeable> get() {
        try {
            Process proc = Runtime.getRuntime().exec(this.command);

            // Read the FFMPEG log and write it to our console using FastLoggingFramework.
            AsyncTask.create(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.logger.debug(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            AsyncTask.create(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.logger.debug(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            return new Pair<>(proc.getOutputStream(), proc::destroy);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
