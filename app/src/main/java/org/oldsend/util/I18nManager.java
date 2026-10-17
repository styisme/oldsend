package org.oldsend.util;

import android.content.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18nManager {
    private static final Map<String, Map<String, String>> dict = new HashMap<>();
    private static String currentLang = "en";

    static {
        // ============ 简体中文 (zh) ============
        Map<String, String> zh = new HashMap<>();
        zh.put("running", "运行中"); zh.put("stopped", "已停止"); zh.put("start", "启动"); zh.put("stop", "停止");
        zh.put("log", "日志"); zh.put("scan", "扫描"); zh.put("select_content", "选择内容"); zh.put("nearby_devices", "附近的设备");
        zh.put("no_device", "暂无设备，请点击扫描"); zh.put("receive_history", "接收记录"); zh.put("settings", "设置");
        zh.put("send", "发送"); zh.put("receive", "接收"); zh.put("file", "文件"); zh.put("media", "媒体");
        zh.put("folder", "文件夹"); zh.put("app", "应用"); zh.put("text", "文本"); zh.put("clipboard", "剪贴板");
        zh.put("not_selected", "未选择文件"); zh.put("auto_save", "自动保存"); zh.put("save_album", "保存到相册");
        zh.put("save_history", "保存到历史记录"); zh.put("use_pin", "启用 PIN 密码"); zh.put("pin_off", "当前 PIN: 未启用");
        zh.put("pin_on", "当前 PIN 码: "); zh.put("network", "网络与自动化"); zh.put("web_enable", "启用网页传输");
        zh.put("auto_start", "开机自动启动"); zh.put("receive_notif", "通过通知栏接收"); zh.put("port", "端口");
        zh.put("timeout", "搜索设备超时"); zh.put("other", "其他"); zh.put("anim", "界面切换动画");
        zh.put("set_name", "修改设备名称"); zh.put("backup", "备份 / 恢复设置"); zh.put("about", "关于 OldSend");
        zh.put("language", "语言 (Language)"); zh.put("follow_system", "跟随系统"); zh.put("cancel", "取消");
        zh.put("confirm", "确定"); zh.put("close", "关闭"); zh.put("clear", "清空"); zh.put("input_ip", "手动输入 IP");
        zh.put("input_text", "发送文本"); zh.put("select_lang", "选择语言"); zh.put("restart_tip", "切换语言成功");
        zh.put("backup_title", "备份与恢复");
        zh.put("backup_export", "导出设置到剪贴板");
        zh.put("backup_import", "从剪贴板导入设置");
        dict.put("zh", zh);

        // ============ 繁体中文 (zh_TW) ============
        Map<String, String> zhTW = new HashMap<>(zh);
        zhTW.put("running", "運行中"); zhTW.put("stopped", "已停止"); zhTW.put("start", "啟動"); zhTW.put("stop", "停止");
        zhTW.put("log", "日誌"); zhTW.put("scan", "掃描"); zhTW.put("select_content", "選擇內容"); zhTW.put("nearby_devices", "附近的設備");
        zhTW.put("no_device", "暫無設備，請點擊掃描"); zhTW.put("receive_history", "接收記錄"); zhTW.put("settings", "設定");
        zhTW.put("send", "發送"); zhTW.put("receive", "接收"); zhTW.put("file", "檔案"); zhTW.put("media", "媒體");
        zhTW.put("folder", "資料夾"); zhTW.put("app", "應用"); zhTW.put("text", "文字"); zhTW.put("clipboard", "剪貼簿");
        zhTW.put("not_selected", "未選擇檔案"); zhTW.put("auto_save", "自動儲存"); zhTW.put("save_album", "儲存到相簿");
        zhTW.put("save_history", "儲存到歷史記錄"); zhTW.put("use_pin", "啟用 PIN 密碼"); zhTW.put("pin_off", "當前 PIN: 未啟用");
        zhTW.put("pin_on", "當前 PIN 碼: "); zhTW.put("network", "網路與自動化"); zhTW.put("web_enable", "啟用網頁傳輸");
        zhTW.put("auto_start", "開機自動啟動"); zhTW.put("receive_notif", "透過通知欄接收"); zhTW.put("port", "連接埠");
        zhTW.put("timeout", "搜尋設備逾時"); zhTW.put("other", "其他"); zhTW.put("anim", "介面切換動畫");
        zhTW.put("set_name", "修改設備名稱"); zhTW.put("backup", "備份 / 恢復設定"); zhTW.put("about", "關於 OldSend");
        zhTW.put("language", "語言 (Language)"); zhTW.put("follow_system", "跟隨系統"); zhTW.put("cancel", "取消");
        zhTW.put("confirm", "確定"); zhTW.put("close", "關閉"); zhTW.put("clear", "清空"); zhTW.put("input_ip", "手動輸入 IP");
        zhTW.put("input_text", "發送文字"); zhTW.put("select_lang", "選擇語言"); zhTW.put("restart_tip", "切換語言成功");
        dict.put("zh_TW", zhTW);

        // ============ 英语 (en) ============
        Map<String, String> en = new HashMap<>();
        en.put("running", "Running"); en.put("stopped", "Stopped"); en.put("start", "Start"); en.put("stop", "Stop");
        en.put("log", "Log"); en.put("scan", "Scan"); en.put("select_content", "Select Content"); en.put("nearby_devices", "Nearby Devices");
        en.put("no_device", "No devices, tap scan"); en.put("receive_history", "Receive History"); en.put("settings", "Settings");
        en.put("send", "Send"); en.put("receive", "Receive"); en.put("file", "File"); en.put("media", "Media");
        en.put("folder", "Folder"); en.put("app", "App"); en.put("text", "Text"); en.put("clipboard", "Clipboard");
        en.put("not_selected", "No file selected"); en.put("auto_save", "Auto Save"); en.put("save_album", "Save to Album");
        en.put("save_history", "Save to History"); en.put("use_pin", "Enable PIN"); en.put("pin_off", "PIN: Disabled");
        en.put("pin_on", "Current PIN: "); en.put("network", "Network & Automation"); en.put("web_enable", "Enable Web Share");
        en.put("auto_start", "Auto Start on Boot"); en.put("receive_notif", "Receive via Notification"); en.put("port", "Port");
        en.put("timeout", "Discovery Timeout"); en.put("other", "Other"); en.put("anim", "UI Animation");
        en.put("set_name", "Device Name"); en.put("backup", "Backup / Restore"); en.put("about", "About OldSend");
        en.put("language", "Language"); en.put("follow_system", "Follow System"); en.put("cancel", "Cancel");
        en.put("confirm", "OK"); en.put("close", "Close"); en.put("clear", "Clear"); en.put("input_ip", "Manual IP");
        en.put("input_text", "Send Text"); en.put("select_lang", "Select Language"); en.put("restart_tip", "Language changed");
        en.put("backup_title", "Backup & Restore");
        en.put("backup_export", "Export to Clipboard");
        en.put("backup_import", "Import from Clipboard");
        dict.put("en", en);

        // ============ 日语 (ja) ============
        Map<String, String> ja = new HashMap<>();
        ja.put("running", "実行中"); ja.put("stopped", "停止"); ja.put("start", "開始"); ja.put("stop", "停止");
        ja.put("log", "ログ"); ja.put("scan", "スキャン"); ja.put("select_content", "コンテンツを選択"); ja.put("nearby_devices", "近くのデバイス");
        ja.put("no_device", "デバイスなし、スキャンしてください"); ja.put("receive_history", "受信履歴"); ja.put("settings", "設定");
        ja.put("send", "送信"); ja.put("receive", "受信"); ja.put("file", "ファイル"); ja.put("media", "メディア");
        ja.put("folder", "フォルダ"); ja.put("app", "アプリ"); ja.put("text", "テキスト"); ja.put("clipboard", "クリップボード");
        ja.put("not_selected", "ファイル未選択"); ja.put("auto_save", "自動保存"); ja.put("save_album", "アルバムに保存");
        ja.put("save_history", "履歴に保存"); ja.put("use_pin", "PINを有効化"); ja.put("pin_off", "PIN: 無効");
        ja.put("pin_on", "現在のPIN: "); ja.put("network", "ネットワークと自動化"); ja.put("web_enable", "Web共有を有効化");
        ja.put("auto_start", "起動時に自動開始"); ja.put("receive_notif", "通知から受信"); ja.put("port", "ポート");
        ja.put("timeout", "検索タイムアウト"); ja.put("other", "その他"); ja.put("anim", "UIアニメーション");
        ja.put("set_name", "デバイス名"); ja.put("backup", "バックアップ / 復元"); ja.put("about", "OldSendについて");
        ja.put("language", "言語 (Language)"); ja.put("follow_system", "システムに従う"); ja.put("cancel", "キャンセル");
        ja.put("confirm", "OK"); ja.put("close", "閉じる"); ja.put("clear", "クリア"); ja.put("input_ip", "手動IP入力");
        ja.put("input_text", "テキスト送信"); ja.put("select_lang", "言語を選択"); ja.put("restart_tip", "言語を変更しました");
        ja.put("backup_title", "バックアップと復元");
        ja.put("backup_export", "クリップボードにエクスポート");
        ja.put("backup_import", "クリップボードからインポート");
        dict.put("ja", ja);

        // ============ 韩语 (ko) ============
        Map<String, String> ko = new HashMap<>();
        ko.put("running", "실행 중"); ko.put("stopped", "중지됨"); ko.put("start", "시작"); ko.put("stop", "중지");
        ko.put("log", "로그"); ko.put("scan", "스캔"); ko.put("select_content", "콘텐츠 선택"); ko.put("nearby_devices", "주변 기기");
        ko.put("no_device", "기기가 없습니다. 스캔하세요"); ko.put("receive_history", "수신 기록"); ko.put("settings", "설정");
        ko.put("send", "보내기"); ko.put("receive", "받기"); ko.put("file", "파일"); ko.put("media", "미디어");
        ko.put("folder", "폴더"); ko.put("app", "앱"); ko.put("text", "텍스트"); ko.put("clipboard", "클립보드");
        ko.put("not_selected", "파일 선택 안 됨"); ko.put("auto_save", "자동 저장"); ko.put("save_album", "앨범에 저장");
        ko.put("save_history", "기록에 저장"); ko.put("use_pin", "PIN 활성화"); ko.put("pin_off", "PIN: 비활성화");
        ko.put("pin_on", "현재 PIN: "); ko.put("network", "네트워크 및 자동화"); ko.put("web_enable", "웹 공유 활성화");
        ko.put("auto_start", "부팅 시 자동 시작"); ko.put("receive_notif", "알림으로 받기"); ko.put("port", "포트");
        ko.put("timeout", "검색 시간 초과"); ko.put("other", "기타"); ko.put("anim", "UI 애니메이션");
        ko.put("set_name", "기기 이름"); ko.put("backup", "백업 / 복원"); ko.put("about", "OldSend 정보");
        ko.put("language", "언어 (Language)"); ko.put("follow_system", "시스템 설정 따름"); ko.put("cancel", "취소");
        ko.put("confirm", "확인"); ko.put("close", "닫기"); ko.put("clear", "지우기"); ko.put("input_ip", "수동 IP 입력");
        ko.put("input_text", "텍스트 보내기"); ko.put("select_lang", "언어 선택"); ko.put("restart_tip", "언어가 변경되었습니다");
        ko.put("backup_title", "백업 및 복원");
        ko.put("backup_export", "클립보드로 내보내기");
        ko.put("backup_import", "클립보드에서 가져오기");
        dict.put("ko", ko);

        // ============ 德语 (de) ============
        Map<String, String> de = new HashMap<>();
        de.put("running", "Läuft"); de.put("stopped", "Gestoppt"); de.put("start", "Start"); de.put("stop", "Stopp");
        de.put("log", "Protokoll"); de.put("scan", "Scannen"); de.put("select_content", "Inhalt wählen"); de.put("nearby_devices", "Geräte in der Nähe");
        de.put("no_device", "Keine Geräte, scannen"); de.put("receive_history", "Empfangsverlauf"); de.put("settings", "Einstellungen");
        de.put("send", "Senden"); de.put("receive", "Empfangen"); de.put("file", "Datei"); de.put("media", "Medien");
        de.put("folder", "Ordner"); de.put("app", "App"); de.put("text", "Text"); de.put("clipboard", "Zwischenablage");
        de.put("not_selected", "Keine Datei gewählt"); de.put("auto_save", "Automatisch speichern"); de.put("save_album", "Im Album speichern");
        de.put("save_history", "Im Verlauf speichern"); de.put("use_pin", "PIN aktivieren"); de.put("pin_off", "PIN: Inaktiv");
        de.put("pin_on", "Aktuelle PIN: "); de.put("network", "Netzwerk & Automatisierung"); de.put("web_enable", "Web-Freigabe");
        de.put("auto_start", "Autostart"); de.put("receive_notif", "Über Benachrichtigung"); de.put("port", "Port");
        de.put("timeout", "Such-Timeout"); de.put("other", "Andere"); de.put("anim", "UI-Animation");
        de.put("set_name", "Gerätename"); de.put("backup", "Sicherung / Wiederherstellung"); de.put("about", "Über OldSend");
        de.put("language", "Sprache"); de.put("follow_system", "Systemsprache"); de.put("cancel", "Abbrechen");
        de.put("confirm", "OK"); de.put("close", "Schließen"); de.put("clear", "Löschen"); de.put("input_ip", "Manuelle IP");
        de.put("input_text", "Text senden"); de.put("select_lang", "Sprache wählen"); de.put("restart_tip", "Sprache geändert");
        de.put("backup_title", "Sicherung & Wiederherstellung");
        de.put("backup_export", "In Zwischenablage exportieren");
        de.put("backup_import", "Aus Zwischenablage importieren");
        dict.put("de", de);

        // ============ 法语 (fr) ============
        Map<String, String> fr = new HashMap<>();
        fr.put("running", "En cours"); fr.put("stopped", "Arrêté"); fr.put("start", "Démarrer"); fr.put("stop", "Arrêter");
        fr.put("log", "Journal"); fr.put("scan", "Scanner"); fr.put("select_content", "Sélectionner"); fr.put("nearby_devices", "Appareils à proximité");
        fr.put("no_device", "Aucun appareil, scannez"); fr.put("receive_history", "Historique"); fr.put("settings", "Paramètres");
        fr.put("send", "Envoyer"); fr.put("receive", "Recevoir"); fr.put("file", "Fichier"); fr.put("media", "Média");
        fr.put("folder", "Dossier"); fr.put("app", "Appli"); fr.put("text", "Texte"); fr.put("clipboard", "Presse-papiers");
        fr.put("not_selected", "Aucun fichier"); fr.put("auto_save", "Sauvegarde auto"); fr.put("save_album", "Sauver dans l'album");
        fr.put("save_history", "Sauver dans l'historique"); fr.put("use_pin", "Activer le PIN"); fr.put("pin_off", "PIN : Désactivé");
        fr.put("pin_on", "PIN actuel : "); fr.put("network", "Réseau & Automatisation"); fr.put("web_enable", "Partage Web");
        fr.put("auto_start", "Démarrage auto"); fr.put("receive_notif", "Via notification"); fr.put("port", "Port");
        fr.put("timeout", "Délai de recherche"); fr.put("other", "Autre"); fr.put("anim", "Animations");
        fr.put("set_name", "Nom de l'appareil"); fr.put("backup", "Sauvegarde / Restauration"); fr.put("about", "À propos d'OldSend");
        fr.put("language", "Langue"); fr.put("follow_system", "Système"); fr.put("cancel", "Annuler");
        fr.put("confirm", "OK"); fr.put("close", "Fermer"); fr.put("clear", "Effacer"); fr.put("input_ip", "IP manuelle");
        fr.put("input_text", "Envoyer texte"); fr.put("select_lang", "Choisir la langue"); fr.put("restart_tip", "Langue modifiée");
        fr.put("backup_title", "Sauvegarde & Restauration");
        fr.put("backup_export", "Exporter vers le presse-papiers");
        fr.put("backup_import", "Importer depuis le presse-papiers");
        dict.put("fr", fr);

        // ============ 西班牙语 (es) ============
        Map<String, String> es = new HashMap<>();
        es.put("running", "Ejecutando"); es.put("stopped", "Detenido"); es.put("start", "Iniciar"); es.put("stop", "Detener");
        es.put("log", "Registro"); es.put("scan", "Escanear"); es.put("select_content", "Seleccionar contenido"); es.put("nearby_devices", "Dispositivos cercanos");
        es.put("no_device", "Sin dispositivos, escanea"); es.put("receive_history", "Historial"); es.put("settings", "Ajustes");
        es.put("send", "Enviar"); es.put("receive", "Recibir"); es.put("file", "Archivo"); es.put("media", "Multimedia");
        es.put("folder", "Carpeta"); es.put("app", "App"); es.put("text", "Texto"); es.put("clipboard", "Portapapeles");
        es.put("not_selected", "Sin archivo"); es.put("auto_save", "Guardar auto"); es.put("save_album", "Guardar en álbum");
        es.put("save_history", "Guardar en historial"); es.put("use_pin", "Activar PIN"); es.put("pin_off", "PIN: Inactivo");
        es.put("pin_on", "PIN actual: "); es.put("network", "Red y Automatización"); es.put("web_enable", "Compartir Web");
        es.put("auto_start", "Inicio automático"); es.put("receive_notif", "Vía notificación"); es.put("port", "Puerto");
        es.put("timeout", "Tiempo de búsqueda"); es.put("other", "Otros"); es.put("anim", "Animaciones");
        es.put("set_name", "Nombre del dispositivo"); es.put("backup", "Copia / Restaurar"); es.put("about", "Acerca de OldSend");
        es.put("language", "Idioma"); es.put("follow_system", "Sistema"); es.put("cancel", "Cancelar");
        es.put("confirm", "Aceptar"); es.put("close", "Cerrar"); es.put("clear", "Limpiar"); es.put("input_ip", "IP manual");
        es.put("input_text", "Enviar texto"); es.put("select_lang", "Seleccionar idioma"); es.put("restart_tip", "Idioma cambiado");
        es.put("backup_title", "Copia de seguridad y restauración");
        es.put("backup_export", "Exportar al portapapeles");
        es.put("backup_import", "Importar desde el portapapeles");
        dict.put("es", es);

        // ============ 俄语 (ru) ============
        Map<String, String> ru = new HashMap<>();
        ru.put("running", "Работает"); ru.put("stopped", "Остановлен"); ru.put("start", "Старт"); ru.put("stop", "Стоп");
        ru.put("log", "Лог"); ru.put("scan", "Сканировать"); ru.put("select_content", "Выбрать контент"); ru.put("nearby_devices", "Устройства рядом");
        ru.put("no_device", "Нет устройств, сканируйте"); ru.put("receive_history", "История"); ru.put("settings", "Настройки");
        ru.put("send", "Отправить"); ru.put("receive", "Получить"); ru.put("file", "Файл"); ru.put("media", "Медиа");
        ru.put("folder", "Папка"); ru.put("app", "Прил."); ru.put("text", "Текст"); ru.put("clipboard", "Буфер");
        ru.put("not_selected", "Файл не выбран"); ru.put("auto_save", "Автосохранение"); ru.put("save_album", "В альбом");
        ru.put("save_history", "В историю"); ru.put("use_pin", "Включить PIN"); ru.put("pin_off", "PIN: Выкл");
        ru.put("pin_on", "Текущий PIN: "); ru.put("network", "Сеть и Автоматизация"); ru.put("web_enable", "Веб-обмен");
        ru.put("auto_start", "Автозапуск"); ru.put("receive_notif", "Через уведомление"); ru.put("port", "Порт");
        ru.put("timeout", "Таймаут поиска"); ru.put("other", "Другое"); ru.put("anim", "Анимации");
        ru.put("set_name", "Имя устройства"); ru.put("backup", "Резервная копия"); ru.put("about", "О OldSend");
        ru.put("language", "Язык"); ru.put("follow_system", "Системный"); ru.put("cancel", "Отмена");
        ru.put("confirm", "ОК"); ru.put("close", "Закрыть"); ru.put("clear", "Очистить"); ru.put("input_ip", "Ввести IP");
        ru.put("input_text", "Отправить текст"); ru.put("select_lang", "Выбрать язык"); ru.put("restart_tip", "Язык изменен");
        ru.put("backup_title", "Резервное копирование");
        ru.put("backup_export", "Экспорт в буфер обмена");
        ru.put("backup_import", "Импорт из буфера обмена");
        dict.put("ru", ru);

        // ============ 葡萄牙语 (pt) ============
        Map<String, String> pt = new HashMap<>();
        pt.put("running", "Rodando"); pt.put("stopped", "Parado"); pt.put("start", "Iniciar"); pt.put("stop", "Parar");
        pt.put("log", "Log"); pt.put("scan", "Escanear"); pt.put("select_content", "Selecionar conteúdo"); pt.put("nearby_devices", "Dispositivos próximos");
        pt.put("no_device", "Nenhum dispositivo, escaneie"); pt.put("receive_history", "Histórico"); pt.put("settings", "Configurações");
        pt.put("send", "Enviar"); pt.put("receive", "Receber"); pt.put("file", "Arquivo"); pt.put("media", "Mídia");
        pt.put("folder", "Pasta"); pt.put("app", "App"); pt.put("text", "Texto"); pt.put("clipboard", "Área de transferência");
        pt.put("not_selected", "Nenhum arquivo"); pt.put("auto_save", "Salvar auto"); pt.put("save_album", "Salvar no álbum");
        pt.put("save_history", "Salvar no histórico"); pt.put("use_pin", "Ativar PIN"); pt.put("pin_off", "PIN: Inativo");
        pt.put("pin_on", "PIN atual: "); pt.put("network", "Rede e Automação"); pt.put("web_enable", "Compartilhar Web");
        pt.put("auto_start", "Início automático"); pt.put("receive_notif", "Via notificação"); pt.put("port", "Porta");
        pt.put("timeout", "Tempo de busca"); pt.put("other", "Outros"); pt.put("anim", "Animações");
        pt.put("set_name", "Nome do dispositivo"); pt.put("backup", "Backup / Restaurar"); pt.put("about", "Sobre o OldSend");
        pt.put("language", "Idioma"); pt.put("follow_system", "Sistema"); pt.put("cancel", "Cancelar");
        pt.put("confirm", "OK"); pt.put("close", "Fechar"); pt.put("clear", "Limpar"); pt.put("input_ip", "IP manual");
        pt.put("input_text", "Enviar texto"); pt.put("select_lang", "Selecionar idioma"); pt.put("restart_tip", "Idioma alterado");
        pt.put("backup_title", "Backup e Restauração");
        pt.put("backup_export", "Exportar para a área de transferência");
        pt.put("backup_import", "Importar da área de transferência");
        dict.put("pt", pt);
    }

    public static void init(Context ctx) {
        String lang = ctx.getSharedPreferences("oldsend", Context.MODE_PRIVATE).getString("app_lang", "system");
        if ("system".equals(lang)) {
            String sysLang = Locale.getDefault().getLanguage();
            String sysCountry = Locale.getDefault().getCountry();
            if ("zh".equals(sysLang)) {
                currentLang = "TW".equals(sysCountry) || "HK".equals(sysCountry) ? "zh_TW" : "zh";
            } else if (dict.containsKey(sysLang)) {
                currentLang = sysLang;
            } else {
                currentLang = "en";
            }
        } else {
            currentLang = lang;
        }
    }

    public static String get(String key) {
        if (dict.containsKey(currentLang) && dict.get(currentLang).containsKey(key)) {
            return dict.get(currentLang).get(key);
        }
        return dict.get("en").containsKey(key) ? dict.get("en").get(key) : key;
    }
}
