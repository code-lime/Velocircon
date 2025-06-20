#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# Read .env: ignore empty lines and lines starting with #
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line; do
    line="$(echo "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    if [[ -n "$line" && ! "$line" =~ ^# ]]; then
      key="${line%%=*}"
      value="${line#*=}"
      key="$(echo "$key" | xargs)"
      value="$(echo "$value" | xargs)"
      export "$key=$value"
    fi
  done < "$ENV_FILE"
else
  echo "Error: .env file not found in $SCRIPT_DIR"
  exit 1
fi

# Check for required variables
if [ -z "$REMOTE_USER" ] || [ -z "$REMOTE_ADDRESS" ]; then
  echo "Missing required environment variables in .env file."
  exit 1
fi

remotePathFromEnv=""
if [ -n "$REMOTE_PATH" ]; then
  remotePathFromEnv="$REMOTE_PATH"
fi

# Validate arguments and provide default for file pattern
if [ "$#" -lt 1 ] && [ -z "$remotePathFromEnv" ]; then
  echo "Usage: ./upload.sh [<file_pattern>] <remote_path>"
  echo "Or specify REMOTE_PATH in your .env file."
  exit 1
fi

if [ "$#" -eq 0 ]; then
  FILE_PATTERN="$SCRIPT_DIR/build/libs/*.jar"
  REMOTE_PATH="$remotePathFromEnv"
elif [ "$#" -eq 1 ]; then
  FILE_PATTERN="$SCRIPT_DIR/build/libs/*.jar"
  REMOTE_PATH="$1"
else
  FILE_PATTERN="$SCRIPT_DIR/$1"
  REMOTE_PATH="$2"
fi

if [ -z "$REMOTE_PATH" ]; then
  echo "Remote path is not specified. Provide as argument or set REMOTE_PATH in .env."
  exit 1
fi

# Find matching files and ignore *-sources.jar if more than one
shopt -s nullglob
files=( $FILE_PATTERN )
shopt -u nullglob

if [ ${#files[@]} -eq 0 ]; then
  echo "No file found matching pattern '$FILE_PATTERN'."
  exit 1
fi

if [ ${#files[@]} -gt 1 ]; then
  filtered=()
  for f in "${files[@]}"; do
    [[ "$f" != *-sources.jar ]] && filtered+=("$f")
  done
  if [ ${#filtered[@]} -eq 1 ]; then
    files=("${filtered[0]}")
  else
    echo "More than one file found matching pattern '$FILE_PATTERN'."
    exit 1
  fi
fi

localFile="${files[0]}"
fileName="$(basename "$localFile")"

# If remotePath is a directory (ends with / or \), append formatted file name manually
if [[ "$REMOTE_PATH" =~ [\\/]$ ]]; then
  baseName="${fileName%%-*}.jar"
  REMOTE_PATH="$REMOTE_PATH$baseName"
fi

# Prepare for SFTP/SSH transfer
if [ -n "$REMOTE_SSH_KEY" ]; then
  if [ ! -f "$REMOTE_SSH_KEY" ]; then
    echo "Specified REMOTE_SSH_KEY file not found: $REMOTE_SSH_KEY"
    exit 1
  fi
  portOpt=""
  [ -n "$REMOTE_PORT" ] && portOpt="-P $REMOTE_PORT"
  scp -i "$REMOTE_SSH_KEY" $portOpt -o StrictHostKeyChecking=no "$localFile" "$REMOTE_USER@$REMOTE_ADDRESS:$REMOTE_PATH"
  scp_status=$?
elif [ -n "$REMOTE_PASSWORD" ]; then
  if ! command -v sshpass >/dev/null 2>&1; then
    echo "sshpass is required for password authentication but it's not installed."
    exit 1
  fi
  portOpt=""
  [ -n "$REMOTE_PORT" ] && portOpt="-P $REMOTE_PORT"
  sshpass -p "$REMOTE_PASSWORD" scp $portOpt -o StrictHostKeyChecking=no "$localFile" "$REMOTE_USER@$REMOTE_ADDRESS:$REMOTE_PATH"
  scp_status=$?
else
  echo "Neither REMOTE_PASSWORD nor REMOTE_SSH_KEY specified in .env file."
  exit 1
fi

if [ $scp_status -eq 0 ]; then
  echo "File upload finished"
else
  echo "File upload failed"
  exit 1
fi
