# 退社時のPC終了方法
# LOCK:     画面ロック
# LOGOFF:   ログオフ
# SHUTDOWN: PC電源オフ
set LEAVING_METHOD "LOCK" -option constant

# 打刻時の確認ダイアログを表示するか否か
# $true: 表示する
# $false: 表示しない
set ASK_CLOCKOUT $true -option constant

. (Join-Path $PSScriptRoot Config.ps1)

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
    # 打刻確認しない
    if (-Not($ASK_CLOCKOUT)) { return $true }

    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup($message, 0, "退社します", 4 + 32) # 4: Yes/Noボタン, 32: Questionダイアログ
    return $result -eq 6 # 6: Yesをクリック
}

function ClockOut() {
    Start-Process -FilePath java.exe -ArgumentList $OPTIONS -Wait -NoNewWindow
}

# リモートデスクトップ接続時は何もしない
if (IsRemoteSession) { 
    exit
}

switch ($LEAVING_METHOD) {
    "LOCK" {
        if (AskClockOut("画面ロックする前に打刻しますか?")) { ClockOut }
        LockPC
    }
    "LOGOFF" {
        if (AskClockOut("ログオフ前に打刻しますか?")) { ClockOut }
        LockPC
    }
    "SHUTDOWN" {
        if (AskClockOut("シャットダウン前に打刻しますか?")) { ClockOut }
        LockPC
    }
}