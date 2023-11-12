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
    private Process listener;

    public final FastLogger logger = new FastLogger("Listener");

    private void doLoop() {
        FastLogger ffmpegLogger = this.logger.createChild("FFMpeg");
        boolean isFirstListen = true;

        while (this.running) {
            this.logger.debug("Proc starting.");

            try {
                this.listener = new ProcessBuilder()
                        .command(
                                "ffmpeg",
                                "-hide_banner",
//                                "-v", "debug",
                                "-f", "flv",
                                "-listen", "1",
//                                "-rw_timeout", "10",
//                                "-timeout", "30",
                                "-rtmp_app", "live",
                                "-i", "rtmp://0.0.0.0:" + Multistream.getConfig().getListener().getPort(),
                                "-c", "copy",
                                "-f", "nut", "pipe:1")
                        .redirectInput(Redirect.PIPE)
                        .redirectOutput(Redirect.PIPE)
                        .redirectError(Redirect.PIPE)
                        .start();

                this.resources.add(this.listener::destroy);

                // Read the FFMPEG log and write it to our console using FastLoggingFramework.
                AsyncTask.create(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(this.listener.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            ffmpegLogger.debug("[FFMPEG] " + line);

                            if (line.contains("Unexpected stream")) {
                                String keyUsed = line.substring(
                                        line.indexOf("Unexpected stream") + "Unexpected stream".length(),
                                        line.lastIndexOf(',')).trim();

                                if (!this.checkStreamKey(keyUsed)) {
                                    this.restart(); // Boot them off.
                                }
                            }
                        }
                    } catch (IOException e) {
                    }
                });

                this.logger.debug("Proc started.");
                if (isFirstListen) {
                    isFirstListen = false;
                    this.logger.info(
                            "Listening on rtmp://127.0.0.1:%d/live, waiting for connection.",
                            Multistream.getConfig().getListener().getPort());
                }

                boolean isFirstPacket = true;
                InputStream nutStream = this.listener.getInputStream();

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

    private boolean checkStreamKey(String keyUsed) {
        if (Multistream.getConfig().getListener().getStreamKey() == null
                || Multistream.getConfig().getListener().getStreamKey().isEmpty()) {
            this.logger.warn("No stream key configured. Ignoring and starting the stream...");
            return true;
        }

        if (Multistream.getConfig().getListener().getStreamKey().equals(keyUsed)) {
            this.logger.info("Stream key authentication passed! Starting the stream...");
            return true;
        } else {
            this.logger.warn("Invalid stream key used: %s. Disconnecting...", keyUsed);
            return false;
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

    public void restart() {
        if (this.listener != null) {
            this.listener.destroy();
        }
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
