package co.casterlabs.multistream;

import java.util.Collections;
import java.util.List;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonClass(exposeAll = true)
public class Config {
    // The file defaults are configured in here.

    private boolean debug = false;
    private boolean disableColoredConsole = false;
    private boolean showPreview = false;
    private boolean recordToFile = false;

    private Listener listener = new Listener();
    private Targets targets = new Targets();

    @Getter
    @ToString
    @JsonClass(exposeAll = true)
    public static class Listener {
        private String streamKey = ""; // Set to blank to disable.
        private int port = 1935;

    }

    @Getter
    @ToString
    @JsonClass(exposeAll = true)
    public static class Targets {
        private List<String> rtmpTargets = Collections.emptyList();
        private List<String> customTargets = Collections.emptyList();

    }

}
