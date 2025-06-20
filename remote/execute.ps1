# Get the script directory
$SCRIPT_DIR = $PSScriptRoot
$envFile = Join-Path $SCRIPT_DIR ".env"

# Check for the existence of the .env file
if (-not (Test-Path $envFile)) {
    Write-Host "Error: .env file not found in $SCRIPT_DIR"
    exit 1
}

# Read the .env file: ignore empty lines and lines starting with #
Get-Content $envFile | ForEach-Object {
    $_.Trim()
} | Where-Object { $_ -and -not $_.StartsWith("#") } | ForEach-Object {
    $parts = $_ -split '=', 2
    if ($parts.Length -eq 2) {
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Variable -Name $key -Value $value -Scope Script
    }
}

# Check for required variables
if (-not $REMOTE_USER -or -not $REMOTE_ADDRESS) {
    Write-Host "Missing required environment variables in .env file."
    exit 1
}

# Allow remote_path in .env (optional)
$remotePathFromEnv = $null
if (Get-Variable -Name REMOTE_PATH -Scope Script -ErrorAction SilentlyContinue) {
    $remotePathFromEnv = $REMOTE_PATH
}

# Validate arguments and provide default for file pattern
if ($args.Count -lt 1 -and -not $remotePathFromEnv) {
    Write-Host "Usage: .\execute.ps1 [<file_pattern>] <remote_path>"
    Write-Host "Or specify REMOTE_PATH in your .env file."
    exit 1
}

if ($args.Count -eq 0) {
    $filePattern = ".\build\libs\*.jar"
    $remotePath = $remotePathFromEnv
} elseif ($args.Count -eq 1) {
    $filePattern = ".\build\libs\*.jar"
    $remotePath = $args[0]
} else {
    $filePattern = $args[0]
    $remotePath = $args[1]
}

if (-not $remotePath) {
    Write-Host "Remote path is not specified. Provide as argument or set REMOTE_PATH in .env."
    exit 1
}

# Find files matching the given pattern and ignore *-sources.jar if more than one
$files = Get-ChildItem -Filter $filePattern -File
if ($files.Count -eq 0) {
    Write-Host "No file found matching pattern '$filePattern'."
    exit 1
}

if ($files.Count -gt 1) {
    $filtered = $files | Where-Object { $_.Name -notlike "*-sources.jar" }
    if ($filtered.Count -eq 1) {
        $files = $filtered
    } else {
        Write-Host "More than one file found matching pattern '$filePattern'."
        exit 1
    }
}

$localFile = $files[0].FullName
$fileName = $files[0].Name

# If remotePath is a directory (ends with / or \), append formatted file name manually
if ($remotePath -match '[\\/]$') {
    $baseName = ($fileName -split '-', 2)[0] + ".jar"
    $remotePath = "$remotePath$baseName"
}

# Ensure the Posh-SSH module is installed
if (-not (Get-Module -ListAvailable -Name Posh-SSH)) {
    Write-Host "Posh-SSH module is not installed. Installing now..."
    try {
        Install-Module -Name Posh-SSH -Scope CurrentUser -Force -AllowClobber
    }
    catch {
        Write-Host "Failed to install Posh-SSH. Please install it manually."
        exit 1
    }
}
Import-Module Posh-SSH

# Prepare SFTP parameters
$SftpParams = @{
    ComputerName = $REMOTE_ADDRESS
    AcceptKey = $true
    ErrorAction = 'Stop'
}
if ($REMOTE_PORT) {
    $SftpParams.Port = [int]$REMOTE_PORT
}

# Prepare credentials: support either REMOTE_PASSWORD or REMOTE_SSH_KEY
$sftpSession = $null
if ($REMOTE_SSH_KEY) {
    # Use key authentication
    if (-not (Test-Path $REMOTE_SSH_KEY)) {
        Write-Host "Specified REMOTE_SSH_KEY file not found: $REMOTE_SSH_KEY"
        exit 1
    }
    $EmptyPassword = [System.Security.SecureString]::new()
    $Credential = New-Object System.Management.Automation.PSCredential($REMOTE_USER, $EmptyPassword)
    $SftpParams.Credential = $Credential
    $SftpParams.KeyFile = $REMOTE_SSH_KEY
    try {
        $sftpSession = New-SFTPSession @SftpParams
    }
    catch {
        Write-Host "Failed to create SFTP session with SSH key: $_"
        exit 1
    }
} elseif ($REMOTE_PASSWORD) {
    # Use password authentication
    $SecurePassword = ConvertTo-SecureString $REMOTE_PASSWORD -AsPlainText -Force
    $Credential = New-Object System.Management.Automation.PSCredential ($REMOTE_USER, $SecurePassword)
    $SftpParams.Credential = $Credential
    try {
        $sftpSession = New-SFTPSession @SftpParams
    }
    catch {
        Write-Host "Failed to create SFTP session: $_"
        exit 1
    }
} else {
    Write-Host "Neither REMOTE_PASSWORD nor REMOTE_SSH_KEY specified in .env file."
    exit 1
}

if (-not $sftpSession) {
    Write-Host "Failed to create SFTP session."
    exit 1
}

# Transfer the file using the UploadFile method and safely close the stream
try {
    try {
        $fileStream = [System.IO.File]::OpenRead($localFile)
        try {
            $sftpSession.Session.UploadFile($fileStream, $remotePath)
        }
        finally {
            $fileStream.Close()
        }
        Write-Host "File upload finished"
    }
    catch {
        Write-Host "File upload failed: $_"
    }
}
finally {
    Remove-SFTPSession -SFTPSession $sftpSession
}
