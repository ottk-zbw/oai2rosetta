#!/bin/bash
#
#
# This scripts do the start/stop/restart of a PlayFramework project with GIT Support
#

### BEGIN vars
APP=hostingsubapp
APPNAME=dspacesubapp
PORT=9020
BASE_DIR=/opt/play-apps
PLAY_VERSION=23
APPLICATION_PATH=${BASE_DIR}/${PLAY_VERSION}/${APP}
PROD_PATH="${APPLICATION_PATH}/target/universal/stage"
CONF_PATH="${PROD_PATH}/conf/prod.conf"




# Specific project configuration environment :
export _JAVA_OPTIONS="-Xms1024m -Xmx1024m -XX:PermSize=512m -XX:MaxPermSize=512m"
### END vars

# Exit immediately if a command exits with a nonzero exit status.
set -e

update() {
    echo "Updating"

    cd $APPLICATION_PATH || exit

    unset GIT_DIR
    # Update repo
    git pull

    cd $APPLICATION_PATH
    # Creating new project (MUST BE ON THE GOOD DIR)
    $APPLICATION_PATH/activator clean compile stage
}

start() {
    echo "Starting server"
    eval $PROD_PATH"/bin/"$APPNAME" -Dhttp.port="$PORT" -Dconfig.file="$CONF_PATH" &"
}

stop() {
    echo "Stopping server"
    if [ -f $PROD_PATH"/RUNNING_PID" ];then
        kill `cat $PROD_PATH"/RUNNING_PID"`
    fi
}

case "$1" in
    start)
        start
    ;;
    stop)
        stop
    ;;
    restart)
        stop
        start
    ;;
    update)
        update
    ;;
    force-restart)
        stop
        update
        start
    ;;
    *)
        echo $"Usage: $0 {start|force-restart|stop|restart|update}"
esac

exit 0
