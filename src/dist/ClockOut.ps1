# �ގЎ���PC�I�����@
# LOCK:     ��ʃ��b�N
# LOGOFF:   ���O�I�t
# SHUTDOWN: PC�d���I�t
set LEAVING_METHOD "LOCK" -option constant

$JAR_PATH = Join-Path $PSScriptRoot kinnosuke_automation_kotlin-*.jar -Resolve
$INI_PATH = Join-Path $PSScriptRoot TimeRecorder.ini
$DB_PATH = Join-Path $PSScriptRoot TimeRecorder.sqlite3

$OPTIONS = [String[]]("-jar","$JAR_PATH","--config=$INI_PATH","--sqlite=$DB_PATH","OUT")

function LockPC() {
    rundll32 user32.dll, LockWorkStation
}

function Logoff() {
    shutdown.exe /l
}

function Shutdown() {
    shutdown.exe /s
}

function AskClockOut($message) {
    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup($message,0,"�ގЂ��܂�",4 + 32)
    return $result -eq 6
}

function ClockOut() {
    Start-Process -FilePath java.exe -ArgumentList $OPTIONS -Wait -NoNewWindow
}

switch ($LEAVING_METHOD) {
    "LOCK" {
        if (AskClockOut("��ʃ��b�N����O�ɑō����܂���?")) { ClockOut }
        LockPC
    }
    "LOGOFF" {
        if (AskClockOut("���O�I�t�O�ɑō����܂���?")) { ClockOut }
        LockPC
    }
    "SHUTDOWN" {
        if (AskClockOut("�V���b�g�_�E���O�ɑō����܂���?")) { ClockOut }
        LockPC
    }
}