#!/bin/bash

mvn exec:java -Dexec.executable="java" -DdisableRequestSignatureCheck=true
