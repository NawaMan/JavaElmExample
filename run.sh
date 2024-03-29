#!/bin/bash

#== DECLARE ==

DEFAULT_PORT=8080

BLD="\e[1m"
CLR="\e[0m"

function show    { println "${BLD}$1${CLR}"; }
function println { printf  "$1\n";           }

function usage {
    println "${BLD}Usage: ${CLR}./$(basename $0) [-h] [-B] [-O] [-p <port>]"
    println "${BLD}  -h        ${CLR} shows usage."
    println "${BLD}  -B        ${CLR} skip the build."
    println "${BLD}  -O        ${CLR} do not open the page in a browser."
    println "${BLD}  -p <port> ${CLR} specify the port number to <port>. Defaulted to 8080."
}

#== START ==

BUILD=true
OPEN=true
DEMO=false
PORT=$DEFAULT_PORT

while getopts ":hBOdp:" OPT; do
  case ${OPT} in
    B) BUILD=false ;;
    O) OPEN=false ;;
    d) DEMO=true ;;
    p) PORT="$OPTARG" ;;
    h)
      show "# Display the usage"
      usage
      exit 0
      ;;
    :)
      show "# Must supply an argument to -$OPTARG." >&2
      usage
      exit 1
      ;;
    ?)
      show "# Invalid option: -${OPTARG}." >&2
      usage
      exit 2
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done


( $BUILD ) && \
    show "# Start building the application ...." && \
    ./build.sh


show "# Starting the application ...."
java -jar target/JavaElmExample.jar --browser="$OPEN" --port=$PORT --demo=$DEMO
