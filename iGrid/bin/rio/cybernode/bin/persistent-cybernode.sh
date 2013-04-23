#!/bin/bash

#/*
# 
# Copyright 2005 Sun Microsystems, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
# 	http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
#*/

# This script starts a persistent Cybernode using the start-persistent-cybernode.config 
# configuration file found in $RIO_HOME/configs. This script also sets the environment 
# to load native libraries from the ~/.rio/native directory.

export RIO_LOG_DIR=~/.rio/logs/
export NATIVE_DIR=~/.rio/native

# Check for running on OS/X 
opSys=`uname -s`
if [ $opSys = "Darwin" ] ; then
    export DYLD_LIBRARY_PATH=$NATIVE_DIR
else
    export LD_LIBRARY_PATH=$NATIVE_DIR
fi

# Check environment
if [ -x "./envcheck.sh" ] ; then 
    ./envcheck.sh
     if [ $? != "0" ] ; then
         exit $?
     fi
fi

classpath="-cp $RIO_HOME/lib/boot.jar:$JINI_HOME/lib/start.jar"
launchTarget=com.sun.jini.start.ServiceStarter
echo "Starting Persistent Cybernode ... "
$JAVA_HOME/bin/java -server $classpath -Djava.security.policy=$RIO_HOME/configs/sorcer.policy -Djava.protocol.handler.pkgs=net.jini.url -DRIO_HOME=$RIO_HOME -DJINI_HOME=$JINI_HOME -DRIO_LOG_DIR=$RIO_LOG_DIR $launchTarget $RIO_HOME/configs/start-persistent-cybernode.config
