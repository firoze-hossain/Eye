#!/usr/bin/env bash
# TrackEye Agent installer - Linux & macOS
#
# What this does:
#   1. Copies the agent jar to a fixed location (~/.trackeye/app)
#   2. Registers it to start automatically on login (systemd --user on Linux,
#      a LaunchAgent on macOS)
#   3. Starts it now and opens the setup page in your browser
#
# Usage:
#   ./install.sh /path/to/TrackEye-2.0.0.jar [server-url]
#
# Example:
#   ./install.sh TrackEye-2.0.0.jar http://192.168.1.50:8080

set -e

JAR_SOURCE="$1"
SERVER_URL="${2:-http://localhost:8080}"

if [ -z "$JAR_SOURCE" ] || [ ! -f "$JAR_SOURCE" ]; then
    echo "Usage: ./install.sh /path/to/TrackEye-2.0.0.jar [server-url]"
    exit 1
fi

INSTALL_DIR="$HOME/.trackeye/app"
mkdir -p "$INSTALL_DIR"
cp "$JAR_SOURCE" "$INSTALL_DIR/TrackEye.jar"

# Config file the jar reads on startup (matches application.properties keys).
cat > "$INSTALL_DIR/application.properties" <<EOF
server.port=8765
trackeye.server.url=${SERVER_URL}
trackeye.storage-path=\${user.home}/TrackEyeData
server.address=127.0.0.1
EOF

OS="$(uname -s)"

if [ "$OS" = "Darwin" ]; then
    # ---- macOS: LaunchAgent ----
    PLIST="$HOME/Library/LaunchAgents/com.trackeye.agent.plist"
    mkdir -p "$HOME/Library/LaunchAgents"
    cat > "$PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>com.trackeye.agent</string>
    <key>ProgramArguments</key>
    <array>
        <string>java</string>
        <string>-jar</string>
        <string>${INSTALL_DIR}/TrackEye.jar</string>
        <string>--spring.config.location=${INSTALL_DIR}/application.properties</string>
    </array>
    <key>RunAtLoad</key><true/>
    <key>KeepAlive</key><true/>
    <key>StandardOutPath</key><string>${INSTALL_DIR}/agent.log</string>
    <key>StandardErrorPath</key><string>${INSTALL_DIR}/agent.log</string>
</dict>
</plist>
EOF
    launchctl unload "$PLIST" 2>/dev/null || true
    launchctl load "$PLIST"
    echo "Installed as a macOS LaunchAgent - it will start automatically on login."
    echo ""
    echo "IMPORTANT (macOS only): grant Screen Recording permission so screenshots"
    echo "capture real content, not a blank image:"
    echo "  System Settings > Privacy & Security > Screen Recording > enable for 'java'"

else
    # ---- Linux: systemd --user service ----
    SYSTEMD_DIR="$HOME/.config/systemd/user"
    mkdir -p "$SYSTEMD_DIR"
    cat > "$SYSTEMD_DIR/trackeye-agent.service" <<EOF
[Unit]
Description=TrackEye Agent

[Service]
ExecStart=/usr/bin/java -jar ${INSTALL_DIR}/TrackEye.jar --spring.config.location=${INSTALL_DIR}/application.properties
Restart=always

[Install]
WantedBy=default.target
EOF
    systemctl --user daemon-reload
    systemctl --user enable --now trackeye-agent.service
    echo "Installed as a systemd user service - it will start automatically on login."
fi

echo ""
echo "Waiting for the agent to start..."
sleep 4

SETUP_URL="http://localhost:8765/setup.html"
echo "Opening $SETUP_URL - paste the email and token your admin gave you."

if command -v xdg-open > /dev/null; then
    xdg-open "$SETUP_URL" 2>/dev/null || true
elif command -v open > /dev/null; then
    open "$SETUP_URL" 2>/dev/null || true
else
    echo "Open this URL manually: $SETUP_URL"
fi
