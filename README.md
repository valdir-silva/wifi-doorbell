# WifiDoorbell 🔔

> Saber que alguém chegou em casa **antes de tocar a campainha**, porque o celular da pessoa se conecta automaticamente ao WiFi.

Android app que monitora a rede WiFi local, detecta quando dispositivos específicos se conectam e envia notificações push (inclusive via FCM para outros aparelhos).

---

## Como funciona

1. O app escaneia a rede WiFi a cada 30 segundos (modo avançado) ou 2–3 minutos (modo simples)
2. Quando detecta um dispositivo monitorado que não estava na rede antes, dispara notificação
3. A notificação chega em todos os aparelhos do usuário via FCM

```
João chegou!
iPhone 14 conectou ao WiFi de casa · há 30s
[Ver dispositivos]  [Ignorar]
```

---

## Arquitetura

```
wifi-doorbell/
├── core/          # Kotlin Multiplatform — modelos, regras, Firestore repos
│   └── src/commonMain/kotlin/...
└── app/           # Android — UI, WifiScanner, Services, Firebase
    └── src/main/kotlin/...
```

### Módulo `core` (KMP)
- `Device`, `NotificationRule`, `ScanResult` — modelos compartilhados
- `ScanResultProcessor` — detecta novos dispositivos, aplica throttle (sem dependência Android)
- `FirestoreDeviceRepository` / `FirestoreRuleRepository` — sync via [firebase-kotlin-sdk (GitLive)](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- SQLDelight para cache offline

### Módulo `app` (Android)
- `WifiScanner` — ping sweep paralelo + leitura de `/proc/net/arp` + lookup OUI offline
- `NetworkMonitorService` — Foreground Service (modo avançado, scan a cada 30s)
- `MonitorWorker` — WorkManager (modo simples, scan a cada 2–3 min)
- `FcmNotificationSender` — escreve em Firestore para trigger de Cloud Function
- Jetpack Compose + Material 3

---

## Modos de Monitoramento

| Modo | Mecanismo | Intervalo | Notificação persistente |
|------|-----------|-----------|------------------------|
| **Avançado** | Foreground Service | 30s | Sim — ação "Modo simples" |
| **Simples** | WorkManager | 2–3 min | Não |

---

## Modo do Aparelho

| Modo | Descrição |
|------|-----------|
| 🏠 **Scanner** | Fica em casa, escaneia a rede, envia FCM |
| 📱 **Receptor** | Recebe notificações, gerencia devices e regras |
| 🔄 **Ambos** (padrão) | Scanner e receptor ao mesmo tempo |

Útil para usar um tablet antigo como scanner dedicado e receber as notificações no celular principal.

---

## Firebase

| Serviço | Uso |
|---------|-----|
| Firestore | Sync de devices, regras, tokens FCM e settings |
| FCM | Push via Cloud Function (server key nunca exposta no app) |
| Authentication | Google Sign-In |
| Crashlytics | Crash reports |
| Analytics | `device_detected`, `notification_sent`, `mode_changed` |
| Cloud Functions | Trigger FCM quando novo documento em `notifications/` |

---

## Permissões

- `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE`
- `ACCESS_FINE_LOCATION` — obrigatório para WifiManager no Android 10+
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE` (Android 14+)
- `POST_NOTIFICATIONS` (Android 13+)
- `RECEIVE_BOOT_COMPLETED`
- `INTERNET`

---

## Roadmap v1

Veja as [issues abertas](../../issues) para o status de implementação.

---

## Apoie o projeto

Se o app te economizou de levantar do sofá, considere apoiar! Veja a tela de Doação dentro do app.

---

## Licença

[MIT](LICENSE)
