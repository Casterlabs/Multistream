package co.casterlabs.multistream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import co.casterlabs.multistream.providers.FFMPEGProvider;
import co.casterlabs.multistream.providers.FileProvider;
import co.casterlabs.multistream.providers.PreviewProvider;
import co.casterlabs.multistream.providers.RTMPProvider;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Multistream {
    private static final File configFile = new File("config.json");

    private static final FastLogger logger = new FastLogger();
    private static final Listener listener = new Listener();

    private static @Getter Config config;

    public static void main(String[] args) throws Exception {
//        ConsoleUtil.summonConsoleWindow();

        new FileWatcher(configFile) {
            @Override
            public void onChange() {
                try {
                    reload();
                    logger.info("Reloaded config!");
                } catch (Throwable t) {
                    logger.severe("Unable to reload config file:\n%s", t);
                }
            }
        }
                .start();

        reload();

        logger.info("Want to support the project? You can do so via our Ko-fi! https://ko-fi.com/Casterlabs");
    }

    private static void reload() throws IOException {
        if (!configFile.exists()) {
            logger.info("Config file doesn't exist, creating a new file.");
            Files.writeString(
                    configFile.toPath(),
                    Rson.DEFAULT
                            .toJson(new Config())
                            .toString(true));
            return;
        }

        // Clear the list of providers.
        listener.providers.clear();

        try {
            config = Rson.DEFAULT.fromJson(Files.readString(configFile.toPath()), Config.class);

            // disableColoredConsole
            FastLoggingFramework.setColorEnabled(!config.isDisableColoredConsole());

            // debug
            FastLoggingFramework.setDefaultLevel(config.isDebug() ? LogLevel.DEBUG : LogLevel.INFO);
            for (FastLogger logger : Arrays.asList(logger, listener.logger)) {
                logger.setCurrentLevel(FastLoggingFramework.getDefaultLevel());
            }

            // showPreview
            if (config.isShowPreview()) {
                listener.providers.add(PreviewProvider.INSTANCE);
            }

            // recordToFile
            if (config.isRecordToFile()) {
                listener.providers.add(FileProvider.INSTANCE);
            }

            // rtmpTargets
            for (String target : config.getTargets().getRtmpTargets()) {
                if (target.startsWith("#"))
                    continue;

                listener.providers.add(new RTMPProvider(target));
            }

            // customTargets
            for (String target : config.getTargets().getCustomTargets()) {
                if (target.startsWith("#"))
                    continue;

                listener.providers.add(new FFMPEGProvider(target));
            }

//            logger.debug("Using config: %s", config);

            listener.close();
            listener.start();
        } catch (JsonParseException e) {
            logger.severe("Unable to parse config file, is it malformed?\n%s", e);
        }
    }

}
