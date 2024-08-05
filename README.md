Archived as of August 8th, 2024. We now offer a [paid service](https://multistream.casterlabs.co). Forks are welcome! :)

# Multistream

This is a small little tool for multistreaming without a huge hassle.

## Prerequisites

-   Requires FFMPEG installed on your path, Google it ;)
-   Requires Java 11 or greater. You can grab that [here](https://adoptium.net/)

## Starting and Stoping

Double click either start.sh (for Unix/macOS (make sure to `chmod +x`!)) or start.bat (for Windows).

While that is open, you can set OBS to stream to `rtmp://localhost:1935` (without a streamkey). Clicking "Start Streaming" in OBS will start all of the configured targets. Modify your bitrate and resolution how you normally would via OBS.

Clicking "Stop Streaming" will cause the configured targets to stop.

## Performance

This program will just copy the audio/video information directly to the targets, which allows for much better performance when compared to other solutions because you're not encoding multiple streams.

## Configuring

The program auto reloads the config when you click save, just make sure to restart your stream if it's active. If the config file doesn't exist a default one will be created.

### debug

Enables debug logging, which can get really spammy really quickly.

### disableColoredConsole

Disables color in the console, useful if your terminal application does not support color.

### showPreview

Opens a preview window which is muted by default. Spam `0` on your keyboard to increase volume.

### recordToFile

Dumps video to a new mkv file.

### rtmpTargets

A list of rtmp server urls to use. Prefix with `#` to disable.

Here's an example of how to stream to Twitch:
`"rtmp://live-dfw.twitch.tv/app/STREAMKEY"`

Here's an example of how to temporarily disable streaming to Twitch:
`"#rtmp://live-dfw.twitch.tv/app/STREAMKEY"`

### customTargets

A list of custom commands to run when the stream starts, video is piped into `stdin` using the `nut` container format. Prefix with `#` to disable.

Here's an example of how to stream to Caffeine via SRT:  
`"ffmpeg -hide_banner -v error -f nut -i pipe:0 -c copy -f mpegts SRT_URL_HERE"`

### Full Example Config

```json
{
    "debug": false,
    "disableColoredConsole": false,
    "showPreview": false,
    "recordToFile": true,
    "rtmpTargets": ["rtmp://live-dfw.twitch.tv/app/STREAM_KEY", "rtmp://livepush.trovo.live/live/STREAM_KEY"],
    "customTargets": []
}
```
