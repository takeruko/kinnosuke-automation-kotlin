# 打刻時の確認ダイアログを表示するか否か
# $true: 表示する
# $false: 表示しない
set ASK_CLOCKIN $true -option constant

. (Join-Path $PSScriptRoot Config.ps1)

function AskClockIn() {
    # 打刻確認しない
    if (-Not($ASK_CLOCKIN)) { return $true }
    
    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup("出勤の打刻をしますか?", 0, "出勤しました", 4 + 32) # 4: Yes/Noボタン, 32: Questionダイアログ
    return $result -eq 6 # 6: Yesをクリック
}

function IsClockedIn() {
    $result = java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --check IN
    return $result -eq "CLOCKED" -or $result -eq "HOLIDAY"
}

# リモートデスクトップ接続時は何もしない
if (IsRemoteSession) { exit }

if (-Not(IsClockedIn)) {
    if (AskClockIn) {
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH IN
    }
    else {
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --without-clock IN
    }
}
