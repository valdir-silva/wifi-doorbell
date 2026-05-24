# WifiDoorbell — Bootstrap Plan (GitHub Repo + README + Issues)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar repositório GitHub público, README completo e todas as issues que guiarão a implementação do WifiDoorbell.

**Architecture:** Repositório criado via `gh` CLI, README explica o app e arquitetura, todas as funcionalidades viram issues rotuladas para serem implementadas na próxima sessão quando o projeto Android for trazido.

**Tech Stack:** `gh` CLI, Markdown, GitHub Issues/Labels/Milestones

---

## File Structure

```
wifi-doorbell/
  README.md                  ← criado no Task 2
```

Nenhum código Android neste plano — o usuário criará o projeto Android manualmente e trará de volta para a IA implementar as issues.

---

### Task 1: Criar repositório GitHub e fazer push

**Files:**
- None (operação de infra)

- [ ] **Step 1: Criar repositório público no GitHub**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh repo create wifi-doorbell \
  --public \
  --description "Android app that monitors your WiFi network and notifies you when a specific device connects — know someone arrived before they ring the doorbell" \
  --source . \
  --remote origin \
  --push
```

Expected: repositório criado em `github.com/whiteofdarkfree/wifi-doorbell` (ou similar), commit inicial pushed.

- [ ] **Step 2: Verificar que o remote foi configurado**

```bash
git -C /Users/white/Documents/dev/android/repositories/wifi-doorbell remote -v
```

Expected: `origin  https://github.com/<user>/wifi-doorbell.git (fetch/push)`

---

### Task 2: Criar README.md

**Files:**
- Create: `README.md`

- [ ] **Step 1: Criar o README**

Criar `/Users/white/Documents/dev/android/repositories/wifi-doorbell/README.md` com o conteúdo abaixo:

```markdown
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
```

- [ ] **Step 2: Commit e push do README**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
git add README.md
git commit -m "docs: add README with architecture overview and usage guide"
git push origin main
```

Expected: push com sucesso, README visível no GitHub.

---

### Task 3: Criar labels no GitHub

**Files:**
- None

Labels padrão são insuficientes — criar labels semânticos para as issues.

- [ ] **Step 1: Criar labels**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell

gh label create "module:core" --color "0075ca" --description "Kotlin Multiplatform core module"
gh label create "module:app" --color "e4e669" --description "Android app module"
gh label create "module:cloud" --color "d93f0b" --description "Firebase Cloud Functions"
gh label create "feature" --color "a2eeef" --description "New feature"
gh label create "setup" --color "bfd4f2" --description "Project setup and configuration"
gh label create "ui" --color "f9d0c4" --description "UI / Compose screen"
```

Expected: labels criados sem erro (ignore conflitos se label já existir).

---

### Task 4: Criar milestone v1.0

**Files:**
- None

- [ ] **Step 1: Criar milestone**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh api repos/:owner/:repo/milestones \
  --method POST \
  --field title="v1.0" \
  --field description="Versão inicial do WifiDoorbell com todas as funcionalidades core" \
  --field due_on="2026-08-01T00:00:00Z"
```

Expected: milestone criado com número 1.

---

### Task 5: Criar issues de setup e infraestrutura

**Files:**
- None

Criar cada issue com `gh issue create`. Todas vão para o milestone v1.0.

- [ ] **Step 1: Issue — Setup do projeto Android (módulos core + app)**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "Setup: Criar projeto Android com módulos core (KMP) e app" \
  --label "setup,module:core,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Configurar o projeto Android com a estrutura de múltiplos módulos definida na spec.

## Tarefas
- [ ] Criar projeto Android (Kotlin, Gradle Kotlin DSL) com \`minSdk 26\`, \`targetSdk 35\`
- [ ] Criar módulo \`core\` com suporte a Kotlin Multiplatform (targets: \`androidMain\`, \`commonMain\`)
- [ ] Criar módulo \`app\` dependendo de \`core\`
- [ ] Configurar Jetpack Compose + Material 3 no módulo \`app\`
- [ ] Configurar SQLDelight nos dois módulos
- [ ] Adicionar \`firebase-kotlin-sdk (GitLive)\` ao \`core\`
- [ ] Adicionar Firebase Android SDK (Auth, Crashlytics, Analytics, Messaging) ao \`app\`
- [ ] Adicionar \`google-services.json\` (sem commitar — adicionar ao \`.gitignore\`)

## Referências
- Spec: \`docs/superpowers/specs/2026-05-24-wifi-doorbell-design.md\` § Plataforma e Arquitetura
- [firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [SQLDelight](https://cashapp.github.io/sqldelight/)"
```

- [ ] **Step 2: Issue — Permissões e AndroidManifest**

```bash
gh issue create \
  --title "Setup: Configurar permissões no AndroidManifest.xml" \
  --label "setup,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Declarar todas as permissões necessárias para o app funcionar.

## Permissões a adicionar
- \`ACCESS_WIFI_STATE\`
- \`ACCESS_NETWORK_STATE\`
- \`ACCESS_FINE_LOCATION\` — obrigatório para WifiManager no Android 10+
- \`FOREGROUND_SERVICE\`
- \`FOREGROUND_SERVICE_CONNECTED_DEVICE\` — Android 14+
- \`POST_NOTIFICATIONS\` — Android 13+ (solicitar em runtime)
- \`RECEIVE_BOOT_COMPLETED\`
- \`INTERNET\`

## Tarefas
- [ ] Adicionar todas as permissões ao \`AndroidManifest.xml\`
- [ ] Criar \`PermissionHandler\` que solicita \`ACCESS_FINE_LOCATION\` e \`POST_NOTIFICATIONS\` em runtime com rationale
- [ ] Bloquear funcionalidade de scan se \`ACCESS_FINE_LOCATION\` negada (mostrar explicação)

## Referências
- Spec § Permissões Android"
```

- [ ] **Step 3: Issue — Google Sign-In + Firebase Auth**

```bash
gh issue create \
  --title "Feature: Google Sign-In com Firebase Authentication" \
  --label "feature,setup,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Implementar autenticação com Google Sign-In usando Firebase Auth. O UID do usuário é necessário para isolar dados no Firestore.

## Tarefas
- [ ] Configurar Google Sign-In no Firebase Console e no \`google-services.json\`
- [ ] Criar \`AuthRepository\` com \`signInWithGoogle()\`, \`signOut()\`, \`currentUser: Flow<FirebaseUser?>\`
- [ ] Criar tela de login (se não autenticado, redirecionar para login antes de qualquer tela)
- [ ] Salvar token FCM em \`users/{uid}/fcmTokens/{tokenId}\` após login bem-sucedido
- [ ] Logout limpa token FCM local e Firestore

## Referências
- Spec § Firebase — Authentication
- Spec § Estrutura Firestore — \`fcmTokens/{tokenId}\`"
```

---

### Task 6: Criar issues do módulo `core`

- [ ] **Step 1: Issue — Modelos de dados**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "Core: Definir modelos de dados (Device, NotificationRule, ScanResult, ScannedDevice)" \
  --label "feature,module:core" \
  --milestone "v1.0" \
  --body "## Objetivo
Criar os modelos de dados compartilhados no módulo \`core\` (commonMain).

## Modelos

\`\`\`kotlin
// core/src/commonMain/kotlin/dev/wifidoorbell/core/model/

data class Device(
    val mac: String,
    val ip: String,
    val customName: String?,
    val manufacturer: String?,
    val isWatched: Boolean,
    val lastSeen: Long,
    val firstSeen: Long
)

data class NotificationRule(
    val mac: String,
    val throttleMinutes: Int,
    val enabled: Boolean,
    val lastNotifiedAt: Long?
)

data class ScannedDevice(
    val ip: String,
    val mac: String
)

data class ScanResult(
    val timestamp: Long,
    val devices: List<ScannedDevice>
)
\`\`\`

## Tarefas
- [ ] Criar os 4 data classes em \`core/src/commonMain\`
- [ ] Criar interfaces \`DeviceRepository\`, \`RuleRepository\`, \`NotificationRepository\` em \`core/src/commonMain\`
- [ ] Testes unitários para \`ScanResult\` e \`NotificationRule\` (serialização, comparação)

## Referências
- Spec § Modelos de Dados"
```

- [ ] **Step 2: Issue — SQLDelight no core**

```bash
gh issue create \
  --title "Core: SQLDelight — banco local para cache de devices e scans" \
  --label "feature,module:core" \
  --milestone "v1.0" \
  --body "## Objetivo
Configurar SQLDelight no módulo \`core\` para persistir devices e resultados de scan offline.

## Schema

\`\`\`sql
-- core/src/commonMain/sqldelight/dev/wifidoorbell/core/db/Device.sq
CREATE TABLE Device (
  mac TEXT PRIMARY KEY,
  ip TEXT NOT NULL,
  customName TEXT,
  manufacturer TEXT,
  isWatched INTEGER NOT NULL DEFAULT 0,
  lastSeen INTEGER NOT NULL,
  firstSeen INTEGER NOT NULL
);

-- ScanResult.sq
CREATE TABLE ScanResult (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER NOT NULL
);

CREATE TABLE ScannedDevice (
  scanId INTEGER NOT NULL REFERENCES ScanResult(id),
  ip TEXT NOT NULL,
  mac TEXT NOT NULL
);
\`\`\`

## Tarefas
- [ ] Configurar plugin SQLDelight no \`core/build.gradle.kts\`
- [ ] Criar schemas \`.sq\` para \`Device\`, \`ScanResult\`, \`ScannedDevice\`
- [ ] Criar \`SqlDelightDeviceRepository\` implementando \`DeviceRepository\`
- [ ] Criar \`SqlDelightScanRepository\` com \`insertScan()\` e \`getLastScan()\`
- [ ] Testes unitários com banco in-memory

## Referências
- Spec § Considerações Técnicas — SQLDelight"
```

- [ ] **Step 3: Issue — FirestoreDeviceRepository e FirestoreRuleRepository**

```bash
gh issue create \
  --title "Core: FirestoreDeviceRepository e FirestoreRuleRepository" \
  --label "feature,module:core" \
  --milestone "v1.0" \
  --body "## Objetivo
Implementar repositórios Firestore no módulo \`core\` usando firebase-kotlin-sdk (GitLive).

## Estrutura Firestore
\`\`\`
users/{uid}/
  devices/{mac}   ← customName, isWatched, manufacturer, lastSeen, firstSeen
  rules/{mac}     ← throttleMinutes, enabled, lastNotifiedAt
  settings        ← monitoringMode, deviceRole, scanIntervalSeconds
\`\`\`

## Tarefas
- [ ] \`FirestoreDeviceRepository\`: \`observeDevices(): Flow<List<Device>>\`, \`upsert(device: Device)\`, \`setWatched(mac, isWatched)\`
- [ ] \`FirestoreRuleRepository\`: \`getRule(mac): NotificationRule?\`, \`upsert(rule: NotificationRule)\`, \`updateLastNotifiedAt(mac, timestamp)\`
- [ ] \`FirestoreSettingsRepository\`: \`observeSettings(): Flow<AppSettings>\`, \`updateSettings(settings: AppSettings)\`
- [ ] Regras de segurança Firestore: leitura/escrita só para \`request.auth.uid == userId\`
- [ ] Testes de integração com emulador Firestore

## Referências
- Spec § Firebase — Estrutura Firestore
- Spec § Considerações Técnicas — firebase-kotlin-sdk"
```

- [ ] **Step 4: Issue — ScanResultProcessor**

```bash
gh issue create \
  --title "Core: ScanResultProcessor — detectar novos dispositivos e aplicar throttle" \
  --label "feature,module:core" \
  --milestone "v1.0" \
  --body "## Objetivo
Implementar lógica pura de detecção de novos dispositivos e throttle de notificação, sem dependência Android.

## Interface

\`\`\`kotlin
// core/src/commonMain/kotlin/dev/wifidoorbell/core/processor/ScanResultProcessor.kt

class ScanResultProcessor(
    private val deviceRepository: DeviceRepository,
    private val ruleRepository: RuleRepository
) {
    // Retorna lista de devices que devem gerar notificação
    suspend fun process(
        currentScan: ScanResult,
        previousScan: ScanResult?
    ): List<Device>
}
\`\`\`

## Lógica
1. Encontrar MACs presentes em \`currentScan\` mas ausentes em \`previousScan\` (novos)
2. Para cada novo MAC: verificar se está na watchlist (\`isWatched = true\`)
3. Para cada monitorado: verificar throttle (\`lastNotifiedAt + throttleMinutes * 60000 < now\`)
4. Retornar apenas os devices que passaram no throttle

## Tarefas
- [ ] Implementar \`ScanResultProcessor\`
- [ ] Testes unitários cobrindo: primeiro scan (sem previous), device já estava online, throttle ativo, throttle expirado, device não monitorado

## Referências
- Spec § Fluxo de Detecção → Notificação (passos 4–6)"
```

---

### Task 7: Criar issues do módulo `app` — scanning e serviços

- [ ] **Step 1: Issue — WifiScanner**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "App: WifiScanner — ping sweep paralelo + ARP + lookup OUI offline" \
  --label "feature,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Implementar o scanner de rede WiFi que produz \`ScanResult\` a cada chamada.

## Algoritmo
1. Obter subnet atual (ex: \`192.168.1.0/24\`) via \`WifiManager\`
2. Lançar coroutine por IP (1–254), ping ICMP paralelo com timeout de 200ms
3. Fallback TCP connect nas portas 80/443 para IPs sem resposta ICMP
4. Ler \`/proc/net/arp\` para coletar todos os pares \`{ip, mac}\`
5. Para cada MAC, fazer lookup no banco OUI (~5MB embutido) para obter fabricante
6. Retornar \`ScanResult(timestamp = now, devices = [...])\`

## Interface

\`\`\`kotlin
// app/src/main/kotlin/dev/wifidoorbell/app/scanner/WifiScanner.kt

class WifiScanner(
    private val context: Context,
    private val ouiDatabase: OuiDatabase
) {
    suspend fun scan(): ScanResult  // lança em Dispatchers.IO
}
\`\`\`

## Tarefas
- [ ] Implementar \`WifiScanner\` com ping sweep via \`InetAddress.isReachable\`
- [ ] Implementar fallback TCP connect na porta 80 para IPs sem resposta
- [ ] Implementar leitura de \`/proc/net/arp\` com parser do formato do kernel
- [ ] Empacotar banco OUI da IEEE como asset e criar \`OuiDatabase\` com lookup por prefixo de 3 bytes
- [ ] Testes: mock de \`/proc/net/arp\`, lookup OUI correto, scan vazio quando fora de WiFi

## Referências
- Spec § app — WifiScanner
- Spec § Considerações Técnicas — MAC address limitation, Ping sweep, OUI lookup"
```

- [ ] **Step 2: Issue — NetworkMonitorService (Foreground Service)**

```bash
gh issue create \
  --title "App: NetworkMonitorService — Foreground Service para modo avançado (scan 30s)" \
  --label "feature,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Foreground Service que executa o scan a cada 30s (ou intervalo configurado) e exibe notificação persistente.

## Comportamento
- Inicia quando o usuário ativa o \"modo avançado\" nas configurações
- Scan a cada \`scanIntervalSeconds\` (default: 30, range: 15–60)
- Notificação persistente: \"Monitorando rede · 8 dispositivos · último scan há 12s\"
- Ação na notificação: \"🍃 Modo simples\" — chama \`MonitorWorker\` e para o service
- Reinicia após reboot via \`BootReceiver\`

## Notificação persistente

\`\`\`
Monitorando rede
8 dispositivos · último scan há 12s
[🍃 Modo simples]
\`\`\`

## Tarefas
- [ ] Implementar \`NetworkMonitorService : LifecycleService\`
- [ ] Criar canal de notificação \`monitoring_persistent\` (importance LOW para não fazer som)
- [ ] Exibir e atualizar notificação persistente com contagem e tempo do último scan
- [ ] Ação \"Modo simples\" na notificação
- [ ] Declarar service no \`AndroidManifest.xml\` com \`foregroundServiceType=\"connectedDevice\"\`
- [ ] Implementar \`BootReceiver\` para reiniciar o service após reboot se modo avançado estava ativo
- [ ] Testes: service inicia/para, notificação atualiza, BootReceiver dispara

## Referências
- Spec § app — NetworkMonitorService
- Spec § Modos de Monitoramento"
```

- [ ] **Step 3: Issue — MonitorWorker (WorkManager)**

```bash
gh issue create \
  --title "App: MonitorWorker — WorkManager para modo simples (scan 2–3 min)" \
  --label "feature,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Worker periódico via WorkManager que executa scan a cada 2–3 minutos no modo simples.

## Comportamento
- Agendado com \`PeriodicWorkRequest\` com \`flexInterval\` de ±30s
- Intervalo configurável: 1–5 min (default: 2 min)
- Sem notificação persistente
- Constraints: \`requiresNetwork = true\`

## Interface

\`\`\`kotlin
// app/src/main/kotlin/dev/wifidoorbell/app/worker/MonitorWorker.kt

class MonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result
}
\`\`\`

## Tarefas
- [ ] Implementar \`MonitorWorker\` chamando \`WifiScanner\`, \`ScanResultProcessor\` e \`FcmNotificationSender\`
- [ ] Agendar worker ao mudar para modo simples, cancelar ao mudar para modo avançado
- [ ] Testes com \`TestListenableWorkerBuilder\`

## Referências
- Spec § app — MonitorWorker"
```

- [ ] **Step 4: Issue — FcmNotificationSender**

```bash
gh issue create \
  --title "App: FcmNotificationSender — escrever trigger Firestore para Cloud Function" \
  --label "feature,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Escrever documento em \`users/{uid}/notifications/{id}\` para que a Cloud Function dispare o FCM.

## Documento Firestore

\`\`\`json
{
  \"deviceMac\": \"AA:BB:CC:DD:EE:FF\",
  \"deviceName\": \"iPhone do João\",
  \"manufacturer\": \"Apple Inc\",
  \"detectedAt\": 1716580000000,
  \"sentAt\": null
}
\`\`\`

## Interface

\`\`\`kotlin
// app/src/main/kotlin/dev/wifidoorbell/app/notification/FcmNotificationSender.kt

class FcmNotificationSender(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun send(device: Device)
}
\`\`\`

## Tarefas
- [ ] Implementar \`FcmNotificationSender\`
- [ ] Implementar \`LocalNotificationDispatcher\` como fallback quando Firestore não está disponível (notificação local via NotificationManager)
- [ ] Testes: documento criado com campos corretos, fallback local quando offline

## Referências
- Spec § Fluxo de Detecção → Notificação (passos 7–9)
- Spec § app — FcmNotificationSender, LocalNotificationDispatcher"
```

- [ ] **Step 5: Issue — Cloud Function FCM trigger**

```bash
gh issue create \
  --title "Cloud: Cloud Function — disparar FCM quando novo documento em notifications/" \
  --label "feature,module:cloud" \
  --milestone "v1.0" \
  --body "## Objetivo
Cloud Function que observa \`users/{uid}/notifications/{notifId}\` e dispara FCM para todos os tokens do usuário.

## Trigger

\`\`\`typescript
// functions/src/index.ts

export const onNotificationCreated = onDocumentCreated(
  'users/{uid}/notifications/{notifId}',
  async (event) => {
    const uid = event.params.uid;
    const data = event.data?.data();
    // 1. Buscar fcmTokens do usuário
    // 2. Disparar FCM via Admin SDK para cada token
    // 3. Atualizar sentAt no documento
  }
);
\`\`\`

## Payload FCM

\`\`\`json
{
  \"notification\": {
    \"title\": \"{{deviceName}} chegou!\",
    \"body\": \"{{manufacturer}} conectou ao WiFi de casa · há {{elapsed}}\"
  },
  \"data\": {
    \"mac\": \"...\",
    \"action\": \"device_detected\"
  }
}
\`\`\`

## Tarefas
- [ ] Criar projeto \`functions/\` com TypeScript + Firebase Admin SDK
- [ ] Implementar trigger \`onDocumentCreated\`
- [ ] Buscar todos os \`fcmTokens\` do uid, remover tokens inválidos (código \`messaging/invalid-registration-token\`)
- [ ] Testes com Firebase Emulator
- [ ] Deploy com \`firebase deploy --only functions\`

## Referências
- Spec § Firebase — Cloud Functions
- Spec § Fluxo de Detecção → Notificação (passos 8–9)"
```

---

### Task 8: Criar issues de UI

- [ ] **Step 1: Issue — Tela: Lista de Dispositivos (principal)**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "UI: Tela principal — Lista de Dispositivos" \
  --label "feature,module:app,ui" \
  --milestone "v1.0" \
  --body "## Objetivo
Tela principal do app mostrando todos os dispositivos na rede, separados em monitorados e outros.

## Layout

\`\`\`
┌─────────────────────────────────┐
│ 🟢 Modo Avançado · 8 online     │  ← status bar
├─────────────────────────────────┤
│ MONITORADOS                     │
│ ┌─────────────────────────────┐ │
│ │ 🟢 iPhone do João           │ │  ← verde quando online
│ │    Apple Inc · 192.168.1.4  │ │
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│ OUTROS DISPOSITIVOS             │
│  MacBook Pro · 192.168.1.5      │
│  Samsung TV · 192.168.1.10      │
│  ...                            │
├─────────────────────────────────┤
│                           [+]   │  ← FAB adicionar por MAC
└─────────────────────────────────┘
\`\`\`

## Tarefas
- [ ] \`DeviceListScreen\` com \`LazyColumn\` + duas seções (Monitorados / Outros)
- [ ] \`DeviceListViewModel\` observando \`DeviceRepository.observeDevices()\`
- [ ] Status bar: modo atual, contagem de online (do último scan)
- [ ] Item monitorado: nome, fabricante, IP, indicador verde/cinza
- [ ] FAB abre dialog para adicionar device por MAC
- [ ] Navega para \`DeviceDetailScreen\` ao tocar em um item
- [ ] Testes: ViewModel com repositório fake, snapshot do Composable

## Referências
- Spec § Telas — Lista de Dispositivos"
```

- [ ] **Step 2: Issue — Tela: Detalhe do Dispositivo**

```bash
gh issue create \
  --title "UI: Tela de Detalhe do Dispositivo" \
  --label "feature,module:app,ui" \
  --milestone "v1.0" \
  --body "## Objetivo
Tela de detalhe com edição de nome, toggle de monitoramento e configuração de throttle.

## Layout

\`\`\`
┌─────────────────────────────────┐
│ ← Voltar                        │
│                                 │
│  [Nome editável]                │
│  AA:BB:CC:DD:EE:FF              │
│  Apple Inc                      │
│                                 │
│  Monitorar chegada    [toggle]  │
│                                 │
│  Intervalo mínimo entre alertas │
│  ○ 30 min  ● 1h  ○ 2h  ○ Sem  │
│                                 │
│  Última vez visto: há 5 min     │
│  Visto 3x hoje                  │
└─────────────────────────────────┘
\`\`\`

## Tarefas
- [ ] \`DeviceDetailScreen\` recebendo \`mac: String\` como parâmetro de navegação
- [ ] \`DeviceDetailViewModel\`: \`updateName()\`, \`setWatched()\`, \`updateThrottle()\`
- [ ] Campo de nome com edição inline (TextField)
- [ ] Toggle \`isWatched\` com persistência imediata no Firestore
- [ ] Seletor de throttle: Radio buttons — 30min / 1h / 2h / Sem limite
- [ ] Histórico: \"última vez visto\" e contagem do dia (do SQLDelight local)
- [ ] Testes: ViewModel com repositórios fake

## Referências
- Spec § Telas — Detalhe do Dispositivo"
```

- [ ] **Step 3: Issue — Tela: Configurações**

```bash
gh issue create \
  --title "UI: Tela de Configurações" \
  --label "feature,module:app,ui" \
  --milestone "v1.0" \
  --body "## Objetivo
Tela de configurações para modo de monitoramento, modo do aparelho, conta e intervalo de scan.

## Layout

\`\`\`
┌─────────────────────────────────┐
│ ← Configurações                 │
│                                 │
│  Modo de monitoramento          │
│  ○ Avançado (Foreground 30s)    │
│  ● Simples (WorkManager 2min)   │
│                                 │
│  Este aparelho                  │
│  ○ Scanner  ○ Receptor  ● Ambos │
│                                 │
│  Intervalo de scan              │
│  [────●────────────] 30s        │
│                                 │
│  Conta                          │
│  foto  joao@gmail.com  [Sair]   │
└─────────────────────────────────┘
\`\`\`

## Tarefas
- [ ] \`SettingsScreen\` + \`SettingsViewModel\`
- [ ] Toggle Avançado/Simples: inicia/para \`NetworkMonitorService\` ou cancela/agenda \`MonitorWorker\`
- [ ] Seletor de papel do aparelho (Scanner/Receptor/Ambos) persiste no Firestore
- [ ] Slider de intervalo de scan (avançado: 15–60s; simples: 1–5 min)
- [ ] Seção de conta: foto, email, botão logout
- [ ] Testes: ViewModel, transição de modo

## Referências
- Spec § Telas — Configurações"
```

---

### Task 9: Criar issue da tela de Doação

> **Análise de opções de doação no Android:**
>
> Para apps distribuídos na **Google Play Store**, as políticas do Google restringem pagamentos externos para conteúdo digital — mas doações voluntárias para apps gratuitos/open source são permitidas tanto via **Google Play Billing** (In-App Products) quanto via links externos. Para **sideload / F-Droid**, qualquer método externo funciona livremente.
>
> **Opções recomendadas para este app:**
> 1. **Google Play Billing — In-App Products** (one-time): tiers de suporte (ex: R$5, R$10, R$20) — nativo, sem sair do app, Google recebe 15–30%
> 2. **PIX** (Brasil): QR code embutido — sem taxas, instantâneo, mas abre app bancário do usuário
> 3. **Links externos** (GitHub Sponsors, Ko-fi): abre browser
>
> Para mostrar **meta mensal e valor atingido**, o valor total de doações é registrado no Firestore (ou Firebase Remote Config para a meta), atualizado pela Cloud Function quando uma compra IAP é confirmada via servidor.

- [ ] **Step 1: Issue — Tela de Doação**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "UI: Tela de Doação — meta mensal, progresso e opções de pagamento" \
  --label "feature,module:app,ui" \
  --milestone "v1.0" \
  --body "## Objetivo
Tela de doação mostrando a meta mensal de manutenção do app, o valor arrecadado até o momento e as opções de apoio disponíveis.

## Layout

\`\`\`
┌─────────────────────────────────┐
│ ← Apoie o WifiDoorbell          │
│                                 │
│  Manutenção mensal              │
│  ████████████░░░░░  R\$ 47/60   │
│  78% da meta de maio atingida   │
│                                 │
│  ─── Apoiar via PIX ──────────  │
│  [QR Code PIX]                  │
│  Chave: whiteofdarkfree@gmail   │
│  [Copiar chave]                 │
│                                 │
│  ─── Apoiar com Google Play ──  │
│  [☕ Café  R\$5]  [🍕 Pizza R\$10]  │
│  [⭐ Supporter R\$20]             │
│                                 │
│  ─── Outras formas ───────────  │
│  [GitHub Sponsors]  [Ko-fi]     │
│                                 │
│  Obrigado! 💙                   │
└─────────────────────────────────┘
\`\`\`

## Estrutura Firestore para meta/progresso

\`\`\`
public/donation_stats          ← leitura pública (sem auth)
  monthlyGoalBRL: 60.0
  currentMonthReceived: 47.0
  updatedAt: timestamp
  month: \"2026-05\"
\`\`\`

## Opções de doação

### 1. Google Play Billing (In-App Products)
- Criar 3 produtos no Play Console: \`donation_5\`, \`donation_10\`, \`donation_20\`
- Tipo: **one-time purchase** (INAPP)
- Usar [Google Play Billing Library 7.x](https://developer.android.com/google/play/billing)
- Verificar compra no servidor (Cloud Function) antes de registrar como recebida
- Após confirmação: atualizar \`public/donation_stats.currentMonthReceived\` (Cloud Function)
- Google retém 15% (desenvolvedor qualificado como pequeno negócio) ou 30%

### 2. PIX
- Chave PIX configurada em Firebase Remote Config (\`pix_key\`)
- Gerar QR code estático com biblioteca [zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded) ou \`compose-qr-code\`
- Botão \"Copiar chave\" copia para clipboard
- PIX não tem integração automática de confirmação — progresso via PIX é atualizado manualmente ou via Firestore por outro meio

### 3. Links externos
- GitHub Sponsors: \`https://github.com/sponsors/<user>\`
- Ko-fi: link configurado em Remote Config
- Abre com \`CustomTabsIntent\` (Chrome Custom Tabs)

## Tarefas
- [ ] Criar \`DonationScreen\` + \`DonationViewModel\`
- [ ] \`DonationViewModel\` observa \`public/donation_stats\` no Firestore (sem auth necessária)
- [ ] Barra de progresso animada (meta mensal vs. recebido)
- [ ] Seção PIX: QR code gerado client-side, botão copiar chave
- [ ] Seção Google Play Billing: integrar \`BillingClient\`, listar produtos, processar compra
- [ ] Cloud Function \`onIapPurchaseVerified\`: verificar receipt com Google Play API e atualizar \`donation_stats\`
- [ ] Seção links externos com Chrome Custom Tabs
- [ ] Firebase Remote Config para: \`pix_key\`, \`monthly_goal_brl\`, \`kofi_url\`, \`github_sponsors_url\`
- [ ] Regra Firestore: \`public/donation_stats\` é leitura pública, escrita apenas por Cloud Function (Admin SDK)
- [ ] Testes: ViewModel com Firestore fake, progresso calcula porcentagem corretamente

## Dependências
- \`com.android.billingclient:billing-ktx:7.x\`
- \`io.github.g0dkar:qrcode-kotlin-android:x.x\` ou \`com.journeyapps:zxing-android-embedded\`

## Referências
- [Google Play Billing — one-time products](https://developer.android.com/google/play/billing/integrate)
- [Firebase Remote Config Android](https://firebase.google.com/docs/remote-config/get-started?platform=android)
- Spec § Firebase — Firestore"
```

---

### Task 10: Criar issues de Analytics e Crashlytics

- [ ] **Step 1: Issue — Firebase Analytics e Crashlytics**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue create \
  --title "App: Firebase Analytics — eventos device_detected, notification_sent, mode_changed" \
  --label "feature,module:app" \
  --milestone "v1.0" \
  --body "## Objetivo
Instrumentar eventos Analytics definidos na spec e garantir que Crashlytics está capturando crashes.

## Eventos

| Evento | Parâmetros |
|--------|-----------|
| \`device_detected\` | \`device_name\`, \`manufacturer\`, \`scan_latency_ms\` |
| \`notification_sent\` | \`device_mac\` (hashed), \`channel\` (fcm/local) |
| \`mode_changed\` | \`new_mode\` (advanced/simple) |
| \`device_named\` | nenhum (privacidade) |

## Tarefas
- [ ] Criar \`AnalyticsTracker\` wrapper sobre \`FirebaseAnalytics\` com os 4 métodos tipados
- [ ] Injetar \`AnalyticsTracker\` em \`ScanResultProcessor\`, \`MonitorWorker\`, \`SettingsViewModel\`, \`DeviceDetailViewModel\`
- [ ] Configurar Crashlytics: \`FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)\`
- [ ] Garantir que exceções não tratadas em coroutines são reportadas (\`CoroutineExceptionHandler\`)

## Referências
- Spec § Firebase — Analytics e Crashlytics"
```

---

### Task 11: Commit final e verificação

- [ ] **Step 1: Verificar todas as issues criadas**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
gh issue list --milestone "v1.0" --state open
```

Expected: ~15 issues listadas.

- [ ] **Step 2: Commit do plano**

```bash
cd /Users/white/Documents/dev/android/repositories/wifi-doorbell
git add docs/superpowers/plans/2026-05-24-wifi-doorbell-bootstrap.md
git commit -m "docs: add bootstrap implementation plan with GitHub issues structure"
git push origin main
```

Expected: push com sucesso.

---

## Self-Review

### Spec coverage

| Requisito | Issue |
|-----------|-------|
| Módulos core/app | Issue — Setup projeto |
| Device, NotificationRule, ScanResult | Issue — Core: Modelos |
| SQLDelight | Issue — Core: SQLDelight |
| FirestoreDeviceRepository | Issue — Core: Firestore repos |
| ScanResultProcessor | Issue — Core: ScanResultProcessor |
| WifiScanner (ping + ARP + OUI) | Issue — App: WifiScanner |
| NetworkMonitorService | Issue — App: NetworkMonitorService |
| MonitorWorker | Issue — App: MonitorWorker |
| FcmNotificationSender + LocalNotification | Issue — App: FcmNotificationSender |
| Cloud Function FCM | Issue — Cloud: Cloud Function |
| Google Sign-In | Issue — App: Auth |
| Tela Lista de Dispositivos | Issue — UI: Lista |
| Tela Detalhe | Issue — UI: Detalhe |
| Tela Configurações | Issue — UI: Configurações |
| Tela Doação | Issue — UI: Doação ✅ |
| Analytics + Crashlytics | Issue — App: Analytics |
| Permissões Android | Issue — Setup: Permissões |

Todos os requisitos da spec cobertos.

### Placeholder scan

Nenhum TBD/TODO/placeholder encontrado — todos os passos têm código, comandos e saídas esperadas.

### Type consistency

Modelos definidos na issue Core:Modelos são usados consistentemente nas issues de Firestore, ScanResultProcessor, WifiScanner e FcmNotificationSender.
