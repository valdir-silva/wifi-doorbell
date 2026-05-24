# WifiDoorbell — Design Spec

**Data:** 2026-05-24  
**Status:** Aprovado

---

## Visão Geral

App Android que monitora a rede WiFi local, detecta quando dispositivos específicos se conectam, e envia notificações (inclusive via FCM para outros aparelhos). Caso de uso principal: saber que alguém chegou em casa antes de tocar a campainha, porque o celular da pessoa se conecta automaticamente ao WiFi da casa.

---

## Plataforma e Arquitetura

**Plataforma:** Android Nativo (Kotlin), com módulo `core` em Kotlin Multiplatform para facilitar adição de iOS no futuro.

**Módulos:**

### `core/` (Kotlin Multiplatform)
- Modelos: `Device`, `NotificationRule`, `ScanResult`, `ScanEvent`
- Interfaces: `DeviceRepository`, `RuleRepository`, `NotificationRepository`
- `ScanResultProcessor`: compara scan atual com anterior, detecta novos dispositivos, aplica throttle — sem dependência de Android
- `FirestoreDeviceRepository` e `FirestoreRuleRepository`: implementações usando [firebase-kotlin-sdk (GitLive)](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- Banco local: SQLDelight (cache offline, comparação de scans sem depender de rede)

### `app/` (Android)
- `WifiScanner`: ping sweep paralelo via coroutines + leitura de `/proc/net/arp` + lookup OUI offline para fabricante
- `NetworkMonitorService`: Foreground Service (modo avançado), scan a cada 30s
- `MonitorWorker`: WorkManager (modo simples), scan a cada 2–3 min
- `FcmNotificationSender`: escreve documento em `users/{uid}/notifications/{id}` para disparar Cloud Function
- `LocalNotificationDispatcher`: fallback de notificação local
- Firebase setup: Auth (Google Sign-In), Crashlytics, Analytics
- UI: Jetpack Compose + Material 3

---

## Modos de Monitoramento

| Modo | Mecanismo | Scan | Notificação persistente |
|------|-----------|------|------------------------|
| **Avançado** | Foreground Service | 30s | Sim — com ação "Modo simples" |
| **Simples** | WorkManager | 2–3 min | Não |

O usuário troca de modo nas Configurações ou tocando na ação da notificação persistente.

---

## Modo do Aparelho

Cada instalação pode operar como:
- **🏠 Scanner**: fica em casa, escaneia a rede, envia FCM
- **📱 Receptor**: recebe notificações, gerencia devices e regras
- **🔄 Ambos** (padrão): scanner e receptor ao mesmo tempo

Útil para rodar num tablet antigo como scanner dedicado e receber notificações no celular principal.

---

## Fluxo de Detecção → Notificação

1. `Service` ou `Worker` dispara `WifiScanner` no intervalo configurado
2. `WifiScanner` pinga todos os IPs do subnet em paralelo e lê `/proc/net/arp` → lista de `{ip, mac}`
3. Lookup OUI offline adiciona fabricante (ex: "Apple Inc")
4. `ScanResultProcessor` (core) compara com scan anterior (SQLDelight local)
5. Novo MAC detectado → verifica watchlist via `DeviceRepository`
6. Está monitorado → verifica regra de throttle (`lastNotifiedAt + throttleMinutes`)
7. Throttle permite → `FcmNotificationSender` escreve em `users/{uid}/notifications/{id}`
8. Cloud Function escuta o documento → busca `fcmTokens` do usuário → dispara FCM via Admin SDK
9. Atualiza `lastNotifiedAt` no Firestore
10. Analytics loga `device_detected` com nome do device e latência do scan

---

## Firebase

| Serviço | Uso |
|---------|-----|
| **Firestore** | Sync de devices, regras, tokens FCM e settings entre aparelhos |
| **FCM** | Push notification disparado via Cloud Function |
| **Authentication** | Google Sign-In — necessário para Firestore e para saber para quem enviar FCM |
| **Crashlytics** | Relatório automático de crashes |
| **Analytics** | Eventos: `device_detected`, `notification_sent`, `mode_changed`, `device_named` |
| **Cloud Functions** | Recebe trigger do Firestore e dispara FCM com Firebase Admin SDK (server key nunca exposta no app) |

### Estrutura Firestore

```
users/{uid}/
  devices/{mac}        ← customName, isWatched, manufacturer, lastSeen, firstSeen
  rules/{mac}          ← throttleMinutes, enabled, lastNotifiedAt
  fcmTokens/{tokenId}  ← token, deviceName, createdAt
  settings             ← monitoringMode, deviceRole, scanIntervalSeconds
  notifications/{id}   ← trigger para Cloud Function disparar FCM
```

Regras de segurança Firestore: leitura e escrita apenas para `request.auth.uid == userId`.

---

## Modelos de Dados

```kotlin
// core/
data class Device(
    val mac: String,           // PK — identificador estável
    val ip: String,
    val customName: String?,
    val manufacturer: String?, // OUI lookup
    val isWatched: Boolean,
    val lastSeen: Long,
    val firstSeen: Long
)

data class NotificationRule(
    val mac: String,           // FK → Device
    val throttleMinutes: Int,  // 0 = sem limite
    val enabled: Boolean,
    val lastNotifiedAt: Long?
)

data class ScannedDevice(
    val ip: String,
    val mac: String
)

data class ScanResult(
    val timestamp: Long,
    val devices: List<ScannedDevice>  // raw, sem enriquecimento
)
```

---

## Telas (Jetpack Compose + Material 3)

### Lista de Dispositivos (tela principal)
- Status bar: modo atual (avançado/simples), contagem de dispositivos online
- Seção "Monitorados": devices com `isWatched = true`, destaque verde quando online
- Seção "Outros dispositivos": restante, exibindo fabricante e IP
- FAB para adicionar device manualmente (por MAC)

### Detalhe do Dispositivo
- Nome editável
- MAC + fabricante
- Toggle "Monitorar chegada"
- Seletor de intervalo de throttle: 30 min / 1h / 2h / Sem limite
- Histórico: última vez visto, quantas vezes hoje

### Configurações
- Toggle modo Avançado / Simples
- Toggle modo do aparelho: Scanner / Receptor / Ambos
- Conta Google (logout)
- Intervalo de scan (avançado: 15–60s; simples: 1–5 min)

---

## Notificações

**FCM — device conectou:**
> **João chegou!**  
> iPhone 14 conectou ao WiFi de casa · há 30s  
> [Ver dispositivos] [Ignorar]

**Persistente (modo avançado):**
> **Monitorando rede**  
> 8 dispositivos · último scan há 12s  
> [🍃 Modo simples]

---

## Permissões Android

- `ACCESS_WIFI_STATE` — ler info da rede atual
- `ACCESS_NETWORK_STATE` — verificar se está no WiFi
- `ACCESS_FINE_LOCATION` — obrigatório para `WifiManager` no Android 10+
- `FOREGROUND_SERVICE` — modo avançado
- `FOREGROUND_SERVICE_CONNECTED_DEVICE` — tipo do Foreground Service (Android 14+)
- `POST_NOTIFICATIONS` — Android 13+
- `RECEIVE_BOOT_COMPLETED` — reiniciar serviço após reboot
- `INTERNET` — Firestore + FCM

---

## Considerações Técnicas

- **MAC address limitation (Android 10+):** O MAC do próprio dispositivo é randomizado, mas `/proc/net/arp` ainda retorna os MACs dos outros dispositivos na rede — é o mecanismo de scanning que será usado.
- **Ping sweep:** ICMP pode ser bloqueado em algumas redes; fallback com TCP connect na porta 80/443 para IPs que não respondem ao ping.
- **OUI lookup:** Banco OUI da IEEE embutido no app (~5MB) para lookup offline do fabricante pelo MAC.
- **SQLDelight:** Banco local em ambos os módulos (`core` para regras/devices, `app` para cache de scans).
- **firebase-kotlin-sdk (GitLive):** Wrapper KMP sobre os SDKs nativos do Firebase. Permite usar Firestore e Auth no módulo `core`. Crashlytics e Analytics ficam no módulo `app` (sem suporte KMP adequado).

---

## Fora de Escopo (v1)

- Notificação quando dispositivo **desconecta** (saiu de casa)
- Suporte a múltiplas redes WiFi
- Histórico completo de presença (apenas "última vez visto")
- iOS (estrutura KMP preparada, implementação futura)
- Widget na home screen
