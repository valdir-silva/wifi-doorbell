import os
import time
import datetime
import urllib.request
import urllib.parse
import json
import threading
import re
from flask import Flask, render_template, request, jsonify

app = Flask(__name__)

CONFIG_FILE = "config.json"
DEVICES_FILE = "devices.json"
MAX_LOGS = 100
logs_buffer = []

# Default config
config = {
    "TARGET_MAC": "",
    "DEVICE_FCM_TOKEN": ""
}
devices_history = {}

def load_config():
    global config, devices_history
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, 'r') as f:
            config = json.load(f)
    if os.path.exists(DEVICES_FILE):
        with open(DEVICES_FILE, 'r') as f:
            devices_history = json.load(f)

def save_config():
    with open(CONFIG_FILE, 'w') as f:
        json.dump(config, f, indent=4)

def save_devices():
    with open(DEVICES_FILE, 'w') as f:
        json.dump(devices_history, f, indent=4)

def get_current_time():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def log(message):
    formatted = f"[{get_current_time()}] {message}"
    print(formatted)
    logs_buffer.append(formatted)
    if len(logs_buffer) > MAX_LOGS:
        logs_buffer.pop(0)

def get_mac_vendor(mac):
    try:
        url = f"https://api.macvendors.com/{urllib.parse.quote(mac)}"
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        response = urllib.request.urlopen(req, timeout=3)
        return response.read().decode('utf-8')
    except Exception:
        return "Dispositivo Desconhecido"

def send_fcm_notification(vendor="Dispositivo"):
    token = config.get("DEVICE_FCM_TOKEN")
    log("Disparando push notification (FCM) para o celular...")
    
    import firebase_admin
    if not firebase_admin._apps:
        log("⚠️ Firebase não está inicializado (serviceAccountKey.json faltando). Ignorando notificação Push.")
        return
    
    if not token or token == "TOKEN_DO_CELULAR_AQUI":
        log("⚠️ Token FCM não configurado! A notificação não será enviada.")
        return

    try:
        from firebase_admin import messaging
        message = messaging.Message(
            notification=messaging.Notification(
                title="Campainha: Visita!",
                body=f"Dispositivo detectado: {vendor}"
            ),
            data={
                "mac": config.get("TARGET_MAC", ""),
                "vendor": vendor,
                "event": "connected"
            },
            token=token,
        )
        response = messaging.send(message)
        log(f"✅ Notificação enviada com sucesso! ID: {response}")
    except Exception as e:
        log(f"❌ Erro ao enviar notificação FCM: {e}")

def scanner_loop():
    log("🚪 Iniciando Scanner WifiDoorbell em background...")
    
    # Initialize Firebase
    try:
        import firebase_admin
        from firebase_admin import credentials
        if not firebase_admin._apps:
            if os.path.exists("serviceAccountKey.json"):
                cred = credentials.Certificate("serviceAccountKey.json")
                firebase_admin.initialize_app(cred)
                log("✅ Firebase inicializado com sucesso.")
            else:
                log("⚠️ serviceAccountKey.json não encontrado. FCM não vai funcionar.")
    except Exception as e:
        log(f"⚠️ Erro ao inicializar o Firebase: {e}")

    is_home = False
    rounds_since_last_log = 0
    SCAN_INTERVAL = 30
    HEARTBEAT_ROUNDS = 10
    
    while True:
        target_mac = config.get("TARGET_MAC", "").strip().lower()
        if not target_mac:
            log("⏳ Aguardando MAC Address ser configurado na interface Web...")
            time.sleep(SCAN_INTERVAL)
            continue
            
        try:
            # Wake up devices
            os.system("ping -c 2 -b 192.168.0.255 > /dev/null 2>&1")
            
            # Read ARP
            arp_output = os.popen("arp -a").read()
            target_seen = target_mac in arp_output.lower()
            
            # Extract all MACs and IPs for history
            current_time = get_current_time()
            devices_updated = False
            
            mac_regex = r"(?:[0-9a-fA-F]{2}[:-]){5}[0-9a-fA-F]{2}"
            ip_regex = r"\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b"
            
            for line in arp_output.splitlines():
                mac_match = re.search(mac_regex, line)
                if not mac_match: continue
                
                mac = mac_match.group(0).replace("-", ":").lower()
                if mac == "ff:ff:ff:ff:ff:ff" or mac == "00:00:00:00:00:00": continue
                
                ip_match = re.search(ip_regex, line)
                ip = ip_match.group(0) if ip_match else "Desconhecido"
                
                if mac not in devices_history:
                    # New device, do OUI lookup
                    vendor = get_mac_vendor(mac)
                    devices_history[mac] = {
                        "mac": mac,
                        "ip": ip,
                        "vendor": vendor,
                        "first_seen": current_time,
                        "last_seen": current_time
                    }
                    devices_updated = True
                    time.sleep(1.1) # Respect api.macvendors.com rate limit
                else:
                    if devices_history[mac].get("ip") != ip or devices_history[mac].get("last_seen") != current_time:
                        devices_history[mac]["ip"] = ip
                        devices_history[mac]["last_seen"] = current_time
                        devices_updated = True
            
            if devices_updated:
                save_devices()
            
            if target_seen and not is_home:
                is_home = True
                vendor = get_mac_vendor(target_mac)
                log(f"🟩 ➡️ [{vendor}] Dispositivo ACABOU DE CONECTAR! Alguém chegou.")
                send_fcm_notification(vendor)
                rounds_since_last_log = 0
                
            elif not target_seen and is_home:
                is_home = False
                log("🟥 ⬅️ Dispositivo desconectou da rede (saiu).")
                rounds_since_last_log = 0
                
            rounds_since_last_log += 1
            if rounds_since_last_log >= HEARTBEAT_ROUNDS:
                status = "Em casa" if is_home else "Fora de casa"
                log(f"⏳ Monitorando MAC {target_mac}... Estado: {status}")
                rounds_since_last_log = 0
                
        except Exception as e:
            log(f"⚠️ Erro no scanner: {e}")
            
        time.sleep(SCAN_INTERVAL)

# --- Flask Routes ---

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/api/config", methods=["GET"])
def get_config():
    return jsonify(config)

@app.route("/api/config", methods=["POST"])
def update_config():
    data = request.json
    config["TARGET_MAC"] = data.get("TARGET_MAC", "")
    config["DEVICE_FCM_TOKEN"] = data.get("DEVICE_FCM_TOKEN", "")
    save_config()
    log(f"⚙️ Configuração atualizada via Web (MAC: {config['TARGET_MAC']})")
    return jsonify({"status": "success"})

@app.route("/api/logs", methods=["GET"])
def get_logs():
    return jsonify(logs_buffer)

@app.route("/api/devices", methods=["GET"])
def get_devices():
    # Return as list sorted by last_seen descending
    devices_list = list(devices_history.values())
    devices_list.sort(key=lambda x: x.get("last_seen", ""), reverse=True)
    return jsonify(devices_list)

if __name__ == "__main__":
    load_config()
    
    # Start scanner thread
    t = threading.Thread(target=scanner_loop, daemon=True)
    t.start()
    
    log("🚀 Servidor Web iniciado na porta 5000")
    log("👉 Acesse http://localhost:5000 no seu navegador")
    
    # Run flask
    app.run(host="0.0.0.0", port=5000, debug=False)
