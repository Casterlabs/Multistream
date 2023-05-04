package co.casterlabs.multistream;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.functional.tuples.Pair;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Listener implements Closeable {
    private boolean running = false;

    public final List<Supplier<Pair<OutputStream, Closeable>>> providers = new LinkedList<>();

    private List<Closeable> resources = new LinkedList<>();
    private List<OutputStream> targets = new LinkedList<>();

    public final FastLogger logger = new FastLogger("Listener");

    private void doLoop() {
        FastLogger ffmpegLogger = this.logger.createChild("FFMpeg");
        boolean isFirstListen = true;

        while (this.running) {
            this.logger.debug("Proc starting.");

            try {
                Process listener = new ProcessBuilder()
                        .command(
                                "ffmpeg",
                                "-hide_banner",
//                                "-v", "error",
                                "-f", "flv",
                                "-listen", "1",
                                "-rw_timeout", "10",
                                "-timeout", "30",
                                "-i", "rtmp://0.0.0.0:1935/live",
                                "-c", "copy",
                                "-f", "nut", "pipe:1")
                        .redirectInput(Redirect.PIPE)
                        .redirectOutput(Redirect.PIPE)
                        .redirectError(Redirect.PIPE)
                        .start();

                this.resources.add(listener::destroy);

                // Read the FFMPEG log and write it to our console using FastLoggingFramework.
                AsyncTask.create(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(listener.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            ffmpegLogger.debug("[FFMPEG] " + line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                this.logger.debug("Proc started.");
                if (isFirstListen) {
                    isFirstListen = false;
                    this.logger.info("Listening on rtmp://127.0.0.1:1935/live, waiting for connection.");
                }

                boolean isFirstPacket = true;
                InputStream nutStream = listener.getInputStream();

                byte[] buf = new byte[10 * 1024]; // 10KB
                int read;
                while ((read = nutStream.read(buf)) != -1) {
                    if (isFirstPacket) {
                        isFirstPacket = false;

                        for (Supplier<Pair<OutputStream, Closeable>> provider : this.providers) {
                            Pair<OutputStream, Closeable> result = provider.get();
                            if (result == null)
                                continue;

                            if (result.a() != null) {
                                this.targets.add(result.a());
                            }

                            if (result.b() != null) {
                                this.resources.add(result.b());
                            }
                        }

                        this.logger.info("Stream started.");
                    }

                    byte[] packet = new byte[read];
                    System.arraycopy(buf, 0, packet, 0, read);

                    for (OutputStream target : this.targets) {
                        target.write(packet);
                    }
                }

                if (!isFirstPacket) { // We haven't gotten information yet.
                    this.logger.info("Stream ended.");
                }
            } catch (IOException e) {
                this.logger.debug(e);
            } finally {
                this.doCleanup();
                this.logger.debug("Proc closed.");
            }
        }
    }

    private void doCleanup() {
        this.resources.forEach(Listener::safeClose);
        this.targets.forEach(Listener::safeClose);

        this.targets.clear();
        this.resources.clear();
    }

    public void start() throws IOException {
        if (this.running)
            return;

        this.running = true;
        AsyncTask.createNonDaemon(this::doLoop);
    }

    @Override
    public void close() {
        if (!this.running)
            return;

        this.running = false;
        this.doCleanup();
    }

    private static void safeClose(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

}
