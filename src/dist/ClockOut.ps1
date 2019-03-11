# �ގЎ���PC�I�����@
# LOCK:     ��ʃ��b�N
# LOGOFF:   ���O�I�t
# SHUTDOWN: PC�d���I�t
set LEAVING_METHOD "LOCK" -option constant

# �ō����̊m�F�_�C�A���O��\�����邩�ۂ�
# $true: �\������
# $false: �\�����Ȃ�
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
    # �ō��m�F���Ȃ�
    if (-Not($ASK_CLOCKOUT)) { return $true }

    $wsobj = new-object -comobject wscript.shell
    $result = $wsobj.popup($message, 0, "�ގЂ��܂�", 4 + 32) # 4: Yes/No�{�^��, 32: Question�_�C�A���O
    return $result -eq 6 # 6: Yes���N���b�N
}

function ClockOut() {
    Start-Process -FilePath java.exe -ArgumentList $OPTIONS -Wait -NoNewWindow
}

# �����[�g�f�X�N�g�b�v�ڑ����͉������Ȃ�
if (IsRemoteSession) { 
    exit
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