set JAR_PATH (Join-Path $PSScriptRoot kinnosuke_automation_kotlin-*.jar -Resolve) -option const
set INI_PATH (Join-Path $PSScriptRoot TimeRecorder.ini) -option const
set DB_PATH (Join-Path $PSScriptRoot TimeRecorder.sqlite3) -option const

$OPTIONS = [String[]]("-jar","$JAR_PATH","--config=$INI_PATH","--sqlite=$DB_PATH","OUT")
function IsRemoteSession() {
    #return $env:SESSIONNAME -like "RDP-*"
    Add-Type -Assembly System.Windows.Forms
    return [System.Windows.Forms.SystemInformation]::TerminalServerSession
}
