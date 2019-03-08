$JAR_PATH = Join-Path $PSScriptRoot kinnosuke_automation_kotlin-*.jar -Resolve
$INI_PATH = Join-Path $PSScriptRoot TimeRecorder.ini
$DB_PATH = Join-Path $PSScriptRoot TimeRecorder.sqlite3

function AskClockIn() {
    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup("�o�΂̑ō������܂���?",0,"�o�΂��܂���",4 + 32)
    return $result -eq 6
}

function IsClockedIn() {
    $OPTS = $OPTIONS + [String[]]("--check","IN")
    $result = java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --check IN
    return $result -eq "CLOCKED" -or $result -eq "HOLIDAY"
}
if (-Not(IsClockedIn)) {
    if (AskClockIn) {
        echo "�͂�"
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH IN
    }
    else {
        echo "������"
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --without-clock IN
    }
}
