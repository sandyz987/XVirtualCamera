# XVirtualCamera（Xposed Virtual Camera Plugin）

A virtual camera module based on the Xposed framework suitable for Android 9.0 and above, written in Kotlin.

Support replacing virtual camera content with "live streaming or video," supported protocols include: http, rtsp, rtmp, rtp, etc.

**You are responsible for all consequences arising from the use of this open-source project.**

[![Download](https://img.shields.io/github/v/release/sandyz987/XVirtualCamera?label=Download)](https://github.com/sandyz987/XVirtualCamera/releases/latest)
[![Stars](https://img.shields.io/github/stars/sandyz987/XVirtualCamera?label=Stars)](https://github.com/sandyz987/XVirtualCamera)
[![License](https://img.shields.io/github/license/sandyz987/XVirtualCamera?label=License)](https://choosealicense.com/licenses/gpl-3.0/)

<img src="preview.gif" width="300px"><img src="preview.png" width="300px">



## Scope of Plugin Functionality

Virtual videos can be applied in the following areas:

- Bilibili preview, recording
- Douyin preview, recording, live broadcast
- Kuaishou preview, recording, live streaming [todo: Currently, the new version has compatibility issues and is prone to crashing, to be resolved]
- WeChat Video Account Preview, Recording
- Xiaomi native camera preview
- WhatsApp video call [New on Sep 18, 2024]

Other plugins are not effective in other places (because they have not been tested, they are disabled for compatibility), if you need to use them, please modify the source code and recompile.



## Usage

You can use **network video**, **video stream**, or **local video** as the video source for the virtual camera.

- 1. Please manually open the plugin in the Xposed Manager and select the scope APP.
- 2. Please edit the network video address in `/storage/emulated/0/Android/data/[the package name of the app you want to use the virtual camera with]/cache/stream.txt`, making sure there are no extra blank lines.
- 3. When `stream.txt` does not exist or its content is empty, use `/storage/emulated/0/Android/data/[package name]/cache/virtual.mp4` as the video source for the virtual camera.

Network video supports streaming media protocols such as http, rtsp, rtmp, rtp, etc., while local video supports video formats such as mp4.

**Recommended solution for streaming:** OBS live streaming + Nginx reverse proxy (for specific tutorials, please search for the keywords "setting up live streaming service with OBS and Nginx"), 
then fill in stream.txt with: rtmp://local IP address:port number/name (for example: rtmp://172.20.10.6:1935/live). The delay is approximately 2 to 3 seconds.


## Disclaimer
For learning and communication purposes only, or to provide a way to upload videos. Please do not play any videos/works of others. **All consequences arising from the use of this open-source project are to be borne by yourself.**


## Thanks

The player can support multiple data sources because it is developed based on [bilibili/ijkplayer](https://github.com/bilibili/ijkplayer).



## Download
[Github Release](https://github.com/sandyz987/XVirtualCamera/releases/latest)



## License

This project is licensed under the [GNU General Public Licence 3.0](https://choosealicense.com/licenses/gpl-3.0/).



## Sponsorship

Maintaining it requires spending my spare time, and I hope you can provide some sponsorship~ I would be very grateful : )

<img src="zfb.jpg" width="300px"><img src="wx.jpg" width="300px">
