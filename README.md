# Android WebView Browser

A lightweight Android browser built using the native **Android WebView engine**.
This project focuses on providing a simple browsing experience with useful features like tabs, downloads, privacy protections, and modern UI controls.

---

## Features

### 🌐 Browsing

* Full WebView-based browsing engine
* Support for modern websites
* Desktop-like user agent support
* Address bar with search support
* DuckDuckGo search integration

### 🗂 Tabs

* Chrome-style tab management
* Visual tab previews
* Close tabs individually
* Tab counter indicator
* Quick tab switching interface

### 📄 File & Media Support

* File upload support
* Camera capture support
* Download detection
* Download manager integration
* Built-in PDF handling with preview or external viewer

### 🔒 Privacy & Security

* HTTPS lock indicator
* Certificate information viewer
* Basic tracker blocking
* Do-Not-Track (DNT) request header
* Third-party cookie restrictions

### 📥 Downloads

* Automatic download detection
* File naming using server headers
* Notification when download completes
* Open downloads directly from the browser

### ⚡ Performance

* Lightweight design
* Optimized WebView settings
* Reduced memory usage
* Private session-style cache behavior

---

## UI Features

* Minimal browser interface
* Navigation controls (Back, Forward, Refresh)
* Tab switcher with thumbnails
* Quick menu panel
* Loading progress bar
* Responsive layout for different screen sizes

---

## Supported URL Schemes

The browser supports external app handling for common links:

* `tel:` – phone dialer
* `mailto:` – email apps
* `sms:` – messaging apps
* `whatsapp://` – WhatsApp
* `geo:` – maps
* `market://` – Play Store

Unknown or unsupported schemes are safely blocked.

---

## Project Structure

```
app/
 ├── java/
 │    └── MainActivity.java
 ├── res/
 │    ├── layout/
 │    ├── drawable/
 │    ├── values/
 │    └── menu/
 └── assets/
      └── indexx.html
```

---

## Requirements

* Android Studio
* Android SDK 21+
* Gradle build system

---

## Building the Project

1. Clone the repository

```
git clone https://github.com/yourusername/android-webview-browser.git
```

2. Open the project in **Android Studio**

3. Build the project

```
Build → Make Project
```

4. Run on an Android device or emulator.

---

## Known Limitations

* WebView does not support all Chromium features
* Some advanced CSS features may behave differently
* Built-in extensions are not supported

---

## Future Improvements

* Ad blocker support
* Better tracker protection
* Bookmark manager
* History management
* Desktop mode toggle
* Improved tab preview performance

---

## License

This project is released under the MIT License.

---

## Author

Developed by **K8ight Web Services**
