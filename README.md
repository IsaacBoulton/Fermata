## !!! Be aware of scam !!! The sites like https :// fermata-auto . com  are 100% scam. This app is free and open source!


## Fermata Media Player
[![Download the latest APK](https://img.shields.io/github/v/release/AndreyPavlenko/Fermata?label=Download%20APK&logo=github&style=for-the-badge)](https://github.com/AndreyPavlenko/Fermata/releases/latest)

## About
Fermata Media Player is a free, open source audio, video and TV player with a simple and intuitive interface. It is focused on playing media files organized in folders and playlists.

Supported features:

* Play media files organized in folders
* IPTV addon with support for XMLTV EPG and Catchup
* Remembers the last played track and position for each folder
* Support for favorites and playlists
* Support for CUE and M3U playlists
* Support for bookmarks
* Audio effects: Equalizer, Bass/Volume Boost and Virtualizer
* Configure audio effects for individual tracks and folders
* Configure playback speed for individual tracks and folders
* Customizable titles and subtitles
* Support for Android Auto
* Support for Android TV
* Show favorites and playlists on Android TV home screen
* Pluggable media engines: MediaPlayer, ExoPlayer and VLC
* Video player with support for subtitles and audio streams (VLC Engine only)

## Building the project
* Download and install the latest Android SDK or Android Studio from https://developer.android.com/studio/
* Set the environment variable ANDROID_SDK_ROOT pointing to the SDK directory
```bash
export ANDROID_SDK_ROOT=<path to android SDK>
```

### Clone the repository
```bash
git clone --recurse-submodules https://github.com/AndreyPavlenko/Fermata.git
cd Fermata
```

### Build AAB
```bash
./gradlew bundleAutoRelease -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.aab
```

### Build APK
```bash
./gradlew :fermata:packageAutoReleaseUniversalApk \
  -PAPP_ID_SFX=.type.your.pkg.sfx.here
find $PWD -name *.apk
```

The default `full` module profile includes every available feature. Edit `module-profiles.gradle`
to choose what the smaller core package contains. Build it with `-PMODULE_PROFILE=core`:

```bash
./gradlew :fermata:packageAutoDebugUniversalApk \
  -PMODULE_PROFILE=core -PABI=arm64-v8a
./gradlew :fermata:packageAutoReleaseUniversalApk \
  -PMODULE_PROFILE=core -PABI=arm64-v8a \
  -PAPP_ID_SFX=.type.your.pkg.sfx.here
```

Core artifacts include `-core` in their filename. Omitting `MODULE_PROFILE` continues to build
the full package.

### Building in docker
```bash
docker run -ti --name Fermata andreypavlenko/fermata
```
Enter the requested key alias and password, when prompted.
Build the required package using the above commands.
To copy the built package to the host machine, open a new terminal and run:
```bash
# To copy AAB
docker cp Fermata:/home/mobiledevops/Fermata/fermata/build/outputs/bundle/autoRelease/ .
# To copy APK
docker cp Fermata:/home/mobiledevops/Fermata/fermata/build/outputs/apk/autoRelease/ .
```
