import os
import time
import datetime
import urllib.request
import json

# === CONFIGURAÇÕES DO MVP ===
TARGET_MAC = "aa:bb:cc:dd:ee:ff" # Coloque o MAC do celular alvo aqui (ex: 11:22:33:44:55:66)
DEVICE_FCM_TOKEN = "TOKEN_DO_CELULAR_AQUI" # Vai vir da interface do App Android no Passo 2
FIREBASE_PROJECT_ID = "SEU_PROJETO_ID" 
# ==============================

SCAN_INTERVAL_SECONDS = 30
HEARTBEAT_ROUNDS = 10 # A cada 10 rodadas (5 min) imprime um log de que está vivo

def get_current_time():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def log(message):
    print(f"[{get_current_time()}] {message}")

def send_fcm_notification():
    log("Disparando push notification (FCM) para o celular...")
    # Aqui no futuro usaremos a lib `firebase-admin` do Python
    # ou uma requisição HTTP v1 para disparar o Push para o DEVICE_FCM_TOKEN.
    # Exemplo estrutural (pseudo-código para o MVP):
    # payload = { "message": { "token": DEVICE_FCM_TOKEN, "notification": { "title": "Campainha!", "body": "Alguém conectou ao Wi-Fi." } } }
    # requests.post(f"https://fcm.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/messages:send", json=payload)
    pass

def main():
    log("🚪 Iniciando Servidor WifiDoorbell...")
    log(f"🔎 Vigiando o MAC: {TARGET_MAC}")
    
    is_home = False
    rounds_since_last_log = 0
    
    while True:
        try:
            # 1. Tenta "acordar" os dispositivos na rede via broadcast ping (Linux)
            os.system("ping -c 2 -b 192.168.0.255 > /dev/null 2>&1")
            
            # 2. Lê a tabela ARP
            arp_output = os.popen("arp -a").read()
            target_seen = TARGET_MAC.lower() in arp_output.lower()
            
            # 3. Lógica de Transição (Onde a mágica acontece)
            if target_seen and not is_home:
                is_home = True
                log("🟩 ➡️ Dispositivo ACABOU DE CONECTAR! Alguém chegou.")
                send_fcm_notification()
                rounds_since_last_log = 0 # Reseta o heartbeat para não atrapalhar o log recente
                
            elif not target_seen and is_home:
                is_home = False
                log("🟥 ⬅️ Dispositivo desconectou da rede (saiu).")
                rounds_since_last_log = 0
                
            # 4. Sinal de vida (Heartbeat) para não floodar o terminal
            rounds_since_last_log += 1
            if rounds_since_last_log >= HEARTBEAT_ROUNDS:
                status = "Em casa" if is_home else "Fora de casa"
                log(f"⏳ Monitorando silenciosamente... Estado atual: {status}")
                rounds_since_last_log = 0
                
        except Exception as e:
            log(f"⚠️ Erro ao escanear: {e}")
            
        # Espera para o próximo ciclo
        time.sleep(SCAN_INTERVAL_SECONDS)

if __name__ == "__main__":
    main()
