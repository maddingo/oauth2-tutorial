#!/usr/bin/env bash
CURRDIR=$(readlink -f `dirname "$0"` )
JAVA_BIN=$(readlink -f `which java` )

#set -x
# This script checks the prerequisites and starts the applications in the correct order.

check_build_targets() {
  # check if the build targets exist
  if [ ! -f "${CURRDIR}/authorization-server/target/authorization-server.jar" ]; then
    echo "auth-server.jar does not exist."
    return 1
  fi
  if [ ! -f "${CURRDIR}/resource-server/target/resource-server.jar" ]; then
    echo "resource-server.jar does not exist."
    return 1
  fi
  if [ ! -f "${CURRDIR}/client-app/target/client-app.jar" ]; then
    echo "client-app.jar does not exist."
    return 1
  fi
}

check_prerequisites() {

  # check if there is a auth-server entry inn your /etc/hosts file
  if ! grep -q "auth-server" /etc/hosts ; then
    echo "Please add the following line to your /etc/hosts file:"
    echo "   127.0.0.1 auth-server"
    echo
    return 1
  fi
  echo "auth-server entry found in /etc/hosts. Continuing..."

  # check if we run java 17
  if [ "$($JAVA_BIN --version | awk 'NR==1 && $2 ~ /^17\.0/ {print 17}')" != "17" ] ; then
    echo "You are not running java 17"
    return 1
  else
    echo "You are running java 17. Continuing..."
  fi
}

start_applications() {
  # start the applications
  echo "Starting the applications..."
  # Define the terminal emulator variable
  TERMINAL="gnome-terminal"
  # Check if running in WSL
  if grep -qi Microsoft /proc/version; then
    # Set's WindowsTerminal as terminal for WSL
    # WSL might not recognize Windows Terminal. It needs to be added to WLS's PATH, and either
    # 1. Direct path: TERMINAL="/mnt/c/Users/<USER>/AppData/Local/Microsoft/WindowsApps/Microsoft.WindowsTerminal_8wekyb3d8bbwe/wt.exe"
    # 2. Add symlink: sudo ln -s /mnt/c/Users/<USER>/AppData/Local/Microsoft/WindowsApps/Microsoft.WindowsTerminal_8wekyb3d8bbwe/wt.exe /usr/local/bin/wt
    TERMINAL="wt";

    $TERMINAL wt -w 0 -d "$(wslpath -w $CURRDIR)" --title "Client Application" bash -c "sleep 5 && java -jar client-app/target/client-app.jar"
    $TERMINAL wt -w 0 -d "$(wslpath -w $CURRDIR)" --title "Resource Server" bash -c "sleep 5 && java -jar resource-server/target/resource-server.jar"
    $TERMINAL wt -w 0 -d "$(wslpath -w $CURRDIR)" --title "Authorization Server" bash -c "java -jar authorization-server/target/authorization-server.jar"
  else
    $TERMINAL -t "Client Application" --working-directory="$CURRDIR" -- bash -c "sleep 5s; $JAVA_BIN -jar client-app/target/client-app.jar"
    $TERMINAL -t "Resource Server" --working-directory="$CURRDIR" -- bash -c "sleep 5s; $JAVA_BIN -jar resource-server/target/resource-server.jar"
    $TERMINAL -t "Authorization Server" --working-directory="$CURRDIR" -- bash -c "$JAVA_BIN -jar authorization-server/target/authorization-server.jar"
  fi
}

if ! check_prerequisites ; then
  exit 1
fi

if ! check_build_targets; then
  echo "Build targets not found. Building applications..."
  echo "The build log is in mvn.log"
  mvn -B -l mvn.log clean install
fi

if ! check_build_targets; then
  echo "Build failed. Please check mvn.log for details."
  exit 1
fi

start_applications
