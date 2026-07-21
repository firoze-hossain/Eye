# Getting TrackEye onto an employee's computer

Real OS installers (a signed .exe/.msi, .dmg, .deb) need code-signing
certificates and per-OS build tooling that only makes sense once you're ready
to ship this outside your own team. Until then, this is the least-friction
path: **you build the jar once, employees run one script.**

## What the admin does once

```bash
cd trackeye-central-frontend/../TrackEye     # the agent project
mvn clean package -DskipTests
# produces target/TrackEye-2.0.0.jar
```

Share that single `.jar` file plus the matching install script with each
employee (email, shared drive, USB - whatever's convenient). They never touch
Maven or the source code.

## What the employee does

**Linux / macOS:**
```bash
chmod +x install-linux-mac.sh
./install-linux-mac.sh TrackEye-2.0.0.jar http://YOUR-SERVER-IP:8080
```

**Windows (PowerShell):**
```powershell
.\install-windows.ps1 -JarPath .\TrackEye-2.0.0.jar -ServerUrl http://YOUR-SERVER-IP:8080
```

That's the entire install. The script:
1. Copies the jar to a fixed folder (`~/.trackeye/app` or `%LOCALAPPDATA%\TrackEye`)
2. Registers it to **start automatically on login** (systemd user service on
   Linux, a LaunchAgent on macOS, a Scheduled Task on Windows) - no more
   remembering to run `mvn spring-boot:run`
3. Starts it immediately and **opens the setup page in the browser**
   (`http://localhost:8765/setup.html`)

The employee pastes the **email + registration token** their admin gave them
(from Settings → "Generate device token", or an invite) and clicks Connect.
From that point it runs silently in the background and survives reboots.

## URL tracking that also works in Incognito/Private windows

The agent reads the browser's address bar directly via AT-SPI (Linux's
accessibility API - the same infrastructure GNOME's own screen reader is
built on). This is the real technique behind "URLs visited" in commercial
monitoring tools: find the address-bar UI element and read its text, rather
than reading the browser's history file (which Private/Incognito windows
deliberately never write to).

**No elevated privilege, no kernel driver, no packet capture, no setcap.**
This runs as your normal desktop user, the same way a screen reader does. On
most GNOME desktops the required library is already installed. If it isn't:

```bash
sudo apt install at-spi2-core
```

That's an entirely ordinary accessibility package - the same one screen
readers and other assistive-technology tools depend on, not anything
security-sensitive. If it's missing, the agent detects that on startup, logs
one message, and simply skips this feature - everything else keeps working.

## macOS-specific note

Screenshot capture needs the Screen Recording permission, or captures come
back blank. The install script prints this reminder, but you'll still need to
manually enable it once:
**System Settings → Privacy & Security → Screen Recording → enable for `java`**

## Uninstalling

**Linux:** `systemctl --user disable --now trackeye-agent.service`
**macOS:** `launchctl unload ~/Library/LaunchAgents/com.trackeye.agent.plist`
**Windows:** `Unregister-ScheduledTask -TaskName "TrackEyeAgent"`

Then delete the install folder (`~/.trackeye/app`, `%LOCALAPPDATA%\TrackEye`).

## The real next step

If you outgrow this (dozens of employees, non-technical users, need for a
proper uninstaller in Add/Remove Programs), the right move is packaging with
`jpackage` (bundled with modern JDKs) to produce a real signed .msi/.dmg/.deb
with a native installer wizard - a self-contained follow-up task once you're
ready to invest in code-signing certificates for each OS.
