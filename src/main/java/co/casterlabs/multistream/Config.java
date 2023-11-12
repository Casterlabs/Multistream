package co.casterlabs.multistream;

import java.util.ArrayList;
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

    private Listener listener;
    private Targets targets;

    @Getter
    @ToString
    @JsonClass(exposeAll = true)
    public class Listener {
        private String streamKey = ""; // Set to blank to disable.
        private int port = 1935;

        public String getRtmpPath() {
            if (this.streamKey == null || this.streamKey.isEmpty()) {
                return "/live";
            } else {
                return "/live/" + this.streamKey;
            }
        }

    }

    @Getter
    @ToString
    @JsonClass(exposeAll = true)
    public class Targets {
        private List<String> rtmpTargets = new ArrayList<>();
        private List<String> customTargets = new ArrayList<>();

    }

}
