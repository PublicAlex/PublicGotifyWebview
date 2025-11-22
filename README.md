# Gotify Enhanced: HTML & Accordion Support 🚀


**Transform your notifications into interactive experiences!**

This is a supercharged version of the Gotify Android client, designed for power users who need more than just plain text. We've unlocked the full potential of your notifications with rich HTML support and interactive accordion layouts.

## ✨ New Features

<p align="center">
  <img src="notifyplus.jpg" width="500" alt="Notify Plus Preview" />
</p>


*   **Rich HTML Rendering**: Send notifications with full HTML support! Use tables, lists, bold, italic, and more to format your messages exactly how you want them.
*   **Interactive Accordions**: Keep your notification feed clean and organized. Use `<details>` and `<summary>` tags to create collapsible sections. Perfect for logs, stack traces, or long automated reports that you can expand with a tap! 
*   **Secure & Fast**: Optimized for performance and security, ensuring your data stays safe while looking great.

> [!WARNING]
> **Security Note**: JavaScript is enabled by default to support rich interactive content. If you plan to receive notifications from untrusted third-party sources, we strongly recommend disabling JavaScript in the source code (`settings.javaScriptEnabled = false` in `ListMessageAdapter.kt`) to prevent potential XSS attacks.

---

## 📦 Installation

### Download & Install

1. Download the latest APK from the [Releases](https://github.com/yourusername/gotify-android-enhanced/releases) page
2. Enable "Install from Unknown Sources" in your Android settings
3. Install the APK
4. Configure your Gotify server connection

> **Note**: This is a modified version of the official Gotify Android client. It's not available on Google Play or F-Droid. For the official version, visit [gotify/android](https://github.com/gotify/android).

### Disable Battery Optimization (Important!)

By default Android kills long running apps as they drain the battery. With enabled battery optimization, Gotify will be killed and you won't receive any notifications.

Here is one way to disable battery optimization for Gotify:

* Open "Settings"
* Search for "Battery Optimization"
* Find "Gotify" and disable battery optimization

See also https://dontkillmyapp.com for phone manufacturer specific instructions to disable battery optimizations.

---

## 🚀 Usage Examples

### Basic HTML Formatting

Send rich notifications using HTML tags in your message body:
```bash
curl -X POST "https://your-gotify-server.com/message?token=YOUR_APP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "System Update",
    "message": "<h3>Update Complete</h3><p>The system has been <strong>successfully updated</strong> to version <code>2.1.0</code></p><ul><li>New features added</li><li>Bug fixes applied</li><li>Performance improved</li></ul>",
    "priority": 5
  }'
```

### Interactive Accordions

Use `<details>` and `<summary>` tags to create collapsible sections:
```bash
curl -X POST "https://your-gotify-server.com/message?token=YOUR_APP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Deployment Report",
    "message": "<h3>✅ Deployment Successful</h3><details><summary>📊 Click to view details</summary><pre>Starting deployment...\nBuilding Docker image...\nPushing to registry...\nDeploying to production...\n✓ All steps completed</pre></details><details><summary>⚙️ Configuration</summary><table><tr><td>Environment</td><td>Production</td></tr><tr><td>Version</td><td>v1.2.3</td></tr><tr><td>Region</td><td>us-east-1</td></tr></table></details>",
    "priority": 7
  }'
```

### Stack Traces and Logs

Perfect for error notifications with collapsible stack traces:
```bash
curl -X POST "https://your-gotify-server.com/message?token=YOUR_APP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "🔴 Application Error",
    "message": "<h3>Error in UserService</h3><p><strong>Type:</strong> NullPointerException</p><p><strong>Time:</strong> 2025-01-15 14:30:22</p><details><summary>📋 Stack Trace</summary><pre>java.lang.NullPointerException: Cannot invoke method on null object\n  at com.example.UserService.getUser(UserService.java:45)\n  at com.example.Controller.handleRequest(Controller.java:123)\n  at javax.servlet.http.HttpServlet.service(HttpServlet.java:750)</pre></details>",
    "priority": 9
  }'
```

### Supported HTML Tags

- **Text formatting**: `<b>`, `<strong>`, `<i>`, `<em>`, `<u>`, `<code>`, `<pre>`
- **Headings**: `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>`
- **Lists**: `<ul>`, `<ol>`, `<li>`
- **Tables**: `<table>`, `<tr>`, `<td>`, `<th>`
- **Interactive**: `<details>`, `<summary>` (collapsible sections)
- **Links**: `<a href="...">`
- **Divisions**: `<div>`, `<span>`, `<p>`, `<br>`

---

## Message Priorities

| Notification | Gotify Priority|
| - | - |
| - | 0 |
| Icon in notification bar | 1 - 3 |
| Icon in notification bar + Sound | 4 - 7 |
| Icon in notification bar + Sound + Vibration | 8 - 10 |

## Building

Use Java 17 and execute the following command to build the apk.
```bash
$ ./gradlew build
```

## Update client

* Run `./gradlew generateSwaggerCode`
* Delete `client/settings.gradle` (client is a gradle sub project and must not have a settings.gradle)
* Delete `repositories` block from `client/build.gradle`
* Delete `implementation "com.sun.xml.ws:jaxws-rt:x.x.x"` from `client/build.gradle`
* Insert missing bracket in `retryingIntercept` method of class `src/main/java/com/github/gotify/client/auth/OAuth`
* Commit changes

## Versioning
We use [SemVer](http://semver.org/) for versioning. For the versions available, see the
[tags on this repository](https://github.com/gotify/android/tags).

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

## Credits & Original Project

This project is a fork of the awesome [Gotify Android](https://github.com/gotify/android) client. All credit for the core functionality goes to the original authors and contributors. We simply added some spicy HTML features on top! 🌶️

Gotify Android connects to [gotify/server](https://github.com/gotify/server) and shows push notifications on new messages.

 [github-action-badge]: https://github.com/gotify/android/workflows/Build/badge.svg
 [github-action]: https://github.com/gotify/android/actions?query=workflow%3ABuild
 [playstore]: https://play.google.com/store/apps/details?id=com.github.gotify
 [fdroid-badge]: https://img.shields.io/f-droid/v/com.github.gotify.svg
 [fdroid]: https://f-droid.org/de/packages/com.github.gotify/
 [fossa-badge]: https://app.fossa.io/api/projects/git%2Bgithub.com%2Fgotify%2Fandroid.svg?type=shield
 [fossa]: https://app.fossa.io/projects/git%2Bgithub.com%2Fgotify%2Fandroid
 [release-badge]: https://img.shields.io/github/release/gotify/android.svg
 [release]: https://github.com/gotify/android/releases/latest