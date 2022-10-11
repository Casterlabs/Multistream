package co.casterlabs.multistream.providers;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.function.Supplier;

import co.casterlabs.commons.functional.tuples.Pair;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PreviewProvider implements Supplier<Pair<OutputStream, Closeable>> {
    public static final Supplier<Pair<OutputStream, Closeable>> INSTANCE = new PreviewProvider();

    @Override
    public Pair<OutputStream, Closeable> get() {
        try {
            Process ffplay = new ProcessBuilder()
                .command(
                    "ffplay",
                    "-hide_banner",
                    "-v", "error",

                    "-x", "1280",
                    "-y", "720",
                    "-window_title", "Casterlabs Multistream Previewer (Press 9 to decrease volume, 0 to increase volume)",
                    "-volume", "0",

                    "-i", "pipe:0"
                )
                .redirectInput(Redirect.PIPE)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)
                .start();

            return new Pair<>(ffplay.getOutputStream(), ffplay::destroy);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
