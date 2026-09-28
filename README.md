# WifiDoorbell 🚪🔔

> Saber que alguém chegou em casa **antes de tocar a campainha**, porque o celular da pessoa se conecta automaticamente ao WiFi.

Um sistema IoT moderno (Cliente-Servidor) projetado para monitorar dispositivos na rede local e notificar via aplicativo Android quando determinados dispositivos se conectam à rede.

---

## 🏗️ Nova Arquitetura: Servidor (Raspberry Pi) + Cliente (Android)

Devido às pesadas restrições de privacidade do Android 10+ (que bloqueiam a leitura da tabela ARP e acesso a endereços MAC), a arquitetura foi aprimorada para o modelo Cliente-Servidor.

### 1. O "Cérebro" (Servidor no Raspberry Pi / Ubuntu Server)
Um script super leve (ex: Python) rodando no seu Raspberry Pi. 
- Ele varre a rede local usando ferramentas nativas de rede (como `arp-scan` ou ping em broadcast).
- Por estar no Linux/Ubuntu, **ele tem acesso irrestrito aos Endereços MAC reais**.
- Detecta instantaneamente quando o "Portão" ou "Celular X" aparece na rede.
- Ao detectar, dispara um alerta (Push Notification via Firebase FCM ou requisição direta) para o aplicativo.

### 2. A "Campainha" (App Cliente Android)
O aplicativo se torna extremamente leve, eficiente e não drena bateria.
- Não precisa rodar em Foreground Service 24/7.
- Recebe Push Notifications quando um dispositivo alvo conecta na rede, tocando um som de campainha mesmo com o celular em repouso.
- Permite configurar no Banco de Dados quais MACs você quer monitorar.

---

## 🚀 Como fazer o MVP (Produto Mínimo Viável)

### Passo 1: O Script do Raspberry Pi (Python)
Crie um script Python no Ubuntu Server que fique rodando em loop (ou via Cron):
```python
import os
import time

TARGET_MAC = "aa:bb:cc:dd:ee:ff" # MAC do dispositivo (ex: portão/celular)

while True:
    # Dispara ping para preencher a tabela ARP
    os.system("ping -c 2 -b 192.168.0.255 > /dev/null 2>&1")
    
    # Lê a tabela ARP do Ubuntu
    arp_output = os.popen("arp -a").read()
    
    if TARGET_MAC in arp_output:
        print("Dispositivo detectado! Enviando notificação...")
        # TODO: Fazer requisição HTTP para o FCM ou para o endpoint do Android
        time.sleep(300) # Throttle (evita spam de notificação por 5 minutos)
    
    time.sleep(30)
```

### Passo 2: O App Android
- Integração básica com **Firebase Cloud Messaging (FCM)**.
- O app exibe o Token do FCM na tela.
- O Script do Raspberry usa a API do FCM passando o Token do celular para disparar o Push.
- Ao receber o Push, o Android invoca o `NotificationManager` e toca o som desejado.

---

## 🛠️ Tecnologias Envolvidas

| Componente | Ferramenta |
|---------|-----|
| **Servidor (Pi)** | Python, `arp-scan`, Bash, Linux |
| **Android App** | Kotlin, Jetpack Compose, Material 3, SQLite (SQLDelight) |
| **Mensageria** | Firebase Cloud Messaging (FCM) |

---

## 📌 Issues e Próximos Passos (Roadmap)

1. [ ] **Criar o script Python do Servidor**: Implementar o loop de scan no Ubuntu Server e a chamada POST para o Firebase HTTP v1 API.
2. [ ] **Remover serviços legados do Android**: Deletar o `NetworkMonitorService` e `WifiScanner` que ficavam ativos no Android, pois não são mais necessários.
3. [ ] **Integrar Firebase no Android**: Configurar o `google-services.json` e implementar a classe `FirebaseMessagingService` para receber os Pushes e emitir a notificação sonora local.
4. [ ] **UI do App**: Criar uma interface simples para copiar o Token do aparelho e colar no código do Raspberry Pi (MVP).

---

## Licença
[MIT](LICENSE)
