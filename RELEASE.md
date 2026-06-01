# Guia de Release — Church Member App

Como gerar e publicar uma nova versão na Google Play. **Nenhum segredo fica neste arquivo** (o repo é público).

---

## Pré-requisitos (uma vez por máquina)

- **Android Studio** + **Android SDK Platform 35** + **Build-Tools 35.0.0**
- **JDK 17** (ou o JBR do Android Studio)

## Arquivos NÃO versionados que precisam existir antes de buildar

Estes estão no `.gitignore` (não vêm no clone). Coloque-os manualmente:

| Arquivo | Onde está | Para quê |
|---|---|---|
| `composeApp/google-services.json` | Firebase Console → projeto `church-management-ibc` → app Android → baixar | Config do Firebase (build falha sem ele) |
| `upload-keystore.jks` | Seu **backup privado** (Google Drive / gerenciador de senhas) | Chave de assinatura do release |
| `keystore.properties` | Recriar (modelo abaixo) com a senha do backup | Senhas da chave para o Gradle |

Modelo de `keystore.properties` (na pasta `mobile/`):

```properties
storeFile=upload-keystore.jks
storePassword=SUA_SENHA_DO_BACKUP
keyAlias=upload
keyPassword=SUA_SENHA_DO_BACKUP
```

> A **chave de upload** é a única peça insubstituível — mantenha o `.jks` + senha em backup seguro.
> Com o **Play App Signing** ativo, a chave de upload pode ser resetada via suporte Google se perdida.

---

## Passo a passo de uma nova versão

1. **Incrementar a versão** em `composeApp/build.gradle.kts` → `defaultConfig`:
   - `versionCode` → sempre **maior** que o anterior (ex.: `1` → `2`). A Play exige.
   - `versionName` → texto visível ao usuário (ex.: `"1.0.0"` → `"1.0.1"`).

2. **Gerar o AAB assinado**:
   ```bash
   cd mobile
   ./gradlew :composeApp:bundleRelease
   ```
   Saída: `composeApp/build/outputs/bundle/release/composeApp-release.aab`

3. **Subir no [Play Console](https://play.google.com/console)**: trilha de teste (interno → fechado) → produção → enviar para revisão.

### Build de debug (para testar localmente, sem assinar para a loja)
```bash
./gradlew :composeApp:assembleDebug      # gera APK de debug
./gradlew :composeApp:installDebug       # instala no emulador/dispositivo conectado
```

---

## Checklist rápido antes de publicar

- [ ] `google-services.json` presente em `composeApp/`
- [ ] `upload-keystore.jks` + `keystore.properties` presentes em `mobile/`
- [ ] `versionCode` incrementado
- [ ] `./gradlew :composeApp:bundleRelease` rodou sem erro
- [ ] AAB subido na Play Console e formulários (privacidade, data safety) preenchidos
