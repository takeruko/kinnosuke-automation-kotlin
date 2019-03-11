# �ō����̊m�F�_�C�A���O��\�����邩�ۂ�
# $true: �\������
# $false: �\�����Ȃ�
set ASK_CLOCKIN $true -option constant

. (Join-Path $PSScriptRoot Config.ps1)

function AskClockIn() {
    # �ō��m�F���Ȃ�
    if (-Not($ASK_CLOCKIN)) { return $true }
    
    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup("�o�΂̑ō������܂���?", 0, "�o�΂��܂���", 4 + 32) # 4: Yes/No�{�^��, 32: Question�_�C�A���O
    return $result -eq 6 # 6: Yes���N���b�N
}

function IsClockedIn() {
    $result = java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --check IN
    return $result -eq "CLOCKED" -or $result -eq "HOLIDAY"
}

# �����[�g�f�X�N�g�b�v�ڑ����͉������Ȃ�
if (IsRemoteSession) { exit }

if (-Not(IsClockedIn)) {
    if (AskClockIn) {
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH IN
    }
    else {
        java.exe -jar $JAR_PATH --config=$INI_PATH --sqlite=$DB_PATH --without-clock IN
    }
}
