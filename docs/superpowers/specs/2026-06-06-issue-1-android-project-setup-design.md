# Issue #1 — Setup: Criar projeto Android com módulos core (KMP) e app

**Data:** 2026-06-06
**Status:** Aprovado
**Issue:** [#1](https://github.com/valdir-silva/wifi-doorbell/issues/1)
**Spec base:** `docs/superpowers/specs/2026-05-24-wifi-doorbell-design.md`

---

## Objetivo

Configurar o projeto Android existente com a estrutura de múltiplos módulos definida na spec: refatorar o módulo `app` (package, minSdk, compileSdk) e criar o módulo `core` com suporte a Kotlin Multiplatform.

---

## Estrutura de Arquivos

```
wifi-doorbell/
├── app/
│   ├── build.gradle.kts          ← refatorado: package, minSdk, deps Firebase Android
│   └── src/main/
│       ├── AndroidManifest.xml   ← namespace atualizado
│       └── java/com/alunando/wifidoorbell/
│           └── MainActivity.kt   ← pacote renomeado
│
├── core/                         ← novo módulo KMP
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/alunando/wifidoorbell/core/
│       └── androidMain/kotlin/com/alunando/wifidoorbell/core/
│
├── settings.gradle.kts           ← include(":core") adicionado
├── build.gradle.kts              ← plugins KMP + SQLDelight declarados
└── gradle/libs.versions.toml     ← versões centralizadas
```

---

## Mudanças no módulo `app`

| Campo | Antes | Depois |
|---|---|---|
| `applicationId` / namespace | `com.example.wifidoorbell` | `com.alunando.wifidoorbell` |
| `minSdk` | 24 | 26 |
| `compileSdk` / `targetSdk` | 36 (preview API) | 35 |
| Dependência em `core` | — | `implementation(project(":core"))` |
| Firebase Android SDK | — | BOM + auth, crashlytics, analytics, messaging |
| Plugin `google-services` | — | aplicado |
| Plugin `firebase-crashlytics` | — | aplicado |

Todos os arquivos em `src/main/java/com/example/wifidoorbell/` são movidos para `src/main/java/com/alunando/wifidoorbell/` com o package atualizado no topo de cada arquivo.

---

## Módulo `core` — Kotlin Multiplatform

### Source sets ativos nesta issue

| Source set | Conteúdo |
|---|---|
| `commonMain` | Modelos, interfaces de repositório, dependências firebase-kotlin-sdk e SQLDelight |
| `androidMain` | `AndroidSqliteDriver` — driver concreto SQLDelight para Android |

iOS não é configurado agora. Um bloco `iosMain` é deixado comentado em `build.gradle.kts` para facilitar adição futura, conforme a spec.

### Android config dentro do `core`

```kotlin
android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    namespace = "com.alunando.wifidoorbell.core"
}
```

---

## Dependências

### `gradle/libs.versions.toml` — versões adicionadas

```toml
[versions]
kotlin = "2.1.21"
kmp-firebase = "2.1.0"          # GitLive firebase-kotlin-sdk
sqldelight = "2.0.2"
coroutines = "1.9.0"
firebase-bom = "33.15.0"
google-services = "4.4.2"
firebase-crashlytics-plugin = "3.0.4"

[libraries]
# core — commonMain
firebase-firestore = { module = "dev.gitlive:firebase-firestore", version.ref = "kmp-firebase" }
firebase-auth-kmp = { module = "dev.gitlive:firebase-auth", version.ref = "kmp-firebase" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

# core — androidMain
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }

# app — Firebase Android SDK
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-auth-android = { module = "com.google.firebase:firebase-auth" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
```

### `core/build.gradle.kts` — estrutura

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    // iosArm64() // descomentado quando iOS for implementado
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth.kmp)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

android {
    namespace = "com.alunando.wifidoorbell.core"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

sqldelight {
    databases {
        create("WifiDoorbellDb") {
            packageName.set("com.alunando.wifidoorbell.core.db")
        }
    }
}
```

### `app/build.gradle.kts` — deps adicionadas

```kotlin
implementation(project(":core"))
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth.android)
implementation(libs.firebase.crashlytics)
implementation(libs.firebase.analytics)
implementation(libs.firebase.messaging)
```

---

## `.gitignore`

Adicionar ao `.gitignore` da raiz:

```
**/google-services.json
```

---

## Critérios de Aceitação

- [ ] `./gradlew :app:assembleDebug` compila sem erros
- [ ] `./gradlew :core:compileDebugKotlinAndroid` compila sem erros
- [ ] Package em todos os arquivos Kotlin é `com.alunando.wifidoorbell`
- [ ] `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35` no `app`
- [ ] `core` aparece como módulo em `settings.gradle.kts`
- [ ] `google-services.json` listado no `.gitignore`
- [ ] Firebase dependencies resolvem sem conflito de versão (BOM garante isso)
