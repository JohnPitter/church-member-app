# Church Member App (Kotlin Multiplatform)

App móvel para membros e equipes autorizadas da igreja, compartilhando o backend Firebase do web app
(`church-management-ibc`). UI escrita uma única vez com **Compose Multiplatform** (Android + iOS).

> **Versão 1.0.4 (código 5):** inclui Coordenação Pedagógica nativa, acessível em **Perfil → Coordenação Pedagógica** para usuários com permissão. Também oferece Login, Início, Eventos, Devocionais, Avisos, Perfil, Blog, Fórum, Projetos, Lives, Oração e Liderança.

## Pedagogia

- Avaliação: instrumento recebido do servidor, cinco ciclos, nove dimensões, momentos, observações e encaminhamentos. Assistidos são vinculados às chamadas existentes.
- Aplicação: consulta dos relatos das diretrizes, separada das avaliações dos assistidos.
- Relatórios: filtros de organização, semestre, ciclo, turma e assistido; indicadores recebidos do backend e histórico carregado sob demanda.
- Android: exportação PDF e DOCX por compartilhamento nativo, permitindo salvar os arquivos no dispositivo. A exportação carrega os dados completos antes de gerar o documento.
- A autorização é verificada nas Cloud Functions. Educadores consultam apenas seus registros; Psicologia exige permissão explícita. Dados de avaliações não são acessados diretamente no Firestore.
- Requer `getDevelopmentEvaluationWorkspace`, além das três funções de avaliação já usadas pelo web, em `southamerica-east1`.
- iOS: código compartilhado, sem nova distribuição iOS; a exportação de arquivos desta versão está disponível no Android.

---

## Arquitetura

```
mobile/
├── settings.gradle.kts
├── build.gradle.kts            # plugins (apply false)
├── gradle/libs.versions.toml   # catálogo de versões
└── composeApp/
    ├── build.gradle.kts        # targets android + iOS, deps
    └── src/
        ├── commonMain/kotlin/com/churchmanagement/mobile/
        │   ├── App.kt                  # raiz: tema, gate de auth, bottom-nav, rotas
        │   ├── di/                     # Koin (AppModule, initKoin)
        │   ├── data/                   # repositórios + DTOs Firestore + mapeadores
        │   ├── domain/                 # modelos consumidos pela UI
        │   ├── navigation/             # Navigator + abas
        │   ├── ui/                     # tema + componentes comuns
        │   ├── util/                   # formatação de datas
        │   └── feature/                # auth, home, events, devotionals, notifications, profile
        ├── androidMain/                # MainActivity, MainApplication, AndroidManifest
        └── iosMain/                    # MainViewController (compila aqui, builda no Mac)
```

- **Backend:** reaproveita 100% o Firebase do web app (Firestore, Auth). Nenhuma regra de
  negócio é reescrita — segurança continua nas **Firestore Rules** + **Cloud Functions**.
- **Estado:** repositórios expõem `Flow`; as telas consomem com `collectAsState` + injeção Koin
  (`koinInject`). Sem ViewModel multiplataforma (evita incompatibilidades de versão).
- **Coleções lidas:** `events`, `devotionals`, `notifications` (mesmos nomes/campos do web app).

---

## Pré-requisitos (Windows — alvo Android)

1. **Android Studio** (Ladybug ou mais recente) — instala Android SDK, Gradle e emulador.
   - Em *Settings → Languages & Frameworks → Android SDK*, instale **Android SDK Platform 35**.
2. **JDK 17** (já presente nesta máquina).

Após instalar, o Android Studio cria `local.properties` apontando para o SDK e gera o
`gradle-wrapper.jar` no primeiro *sync*. (O jar não é versionado.)

---

## Configurar o Firebase (obrigatório antes do build)

O plugin `google-services` **falha o build** se o arquivo abaixo não existir.

1. No [Firebase Console](https://console.firebase.google.com/) → projeto **church-management-ibc**.
2. *Project settings → Your apps → Add app → Android*.
   - **Package name:** `com.churchmanagement.mobile`
3. Baixe o **`google-services.json`** e coloque em:
   ```
   mobile/composeApp/google-services.json
   ```
4. Em *Authentication → Sign-in method*, garanta que **E-mail/senha** está habilitado
   (o login do app usa e-mail/senha).

> O `google-services.json` está no `.gitignore` — cada desenvolvedor baixa o seu.

---

## Rodar no Android

Pelo Android Studio: abra a pasta `mobile/`, aguarde o *Gradle sync*, escolha o emulador/dispositivo
e rode a configuração **composeApp**.

Pela linha de comando (o wrapper já está incluído):

```bash
cd mobile
# JAVA_HOME pode apontar para o JBR do Android Studio (Java 21) ou um JDK 17+
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :composeApp:assembleDebug        # gera o APK de debug
./gradlew :composeApp:installDebug         # instala no dispositivo/emulador conectado
```

APK gerado em: `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (~23 MB).

> **Build verificado** nesta máquina (Windows): `commonMain`, target Android e APK de debug
> compilam/montam com sucesso. Requer Android SDK **Platform 35** + **Build-Tools 35.0.0**.

> Use o `google-services.json` real do projeto `church-management-ibc`. Esse arquivo é local e não é versionado.

### APK/AAB de release

```bash
./gradlew :composeApp:assembleRelease      # APK (precisa de keystore para assinar)
./gradlew :composeApp:bundleRelease        # AAB para a Play Store
```

> O release usa a assinatura configurada em `keystore.properties`; consulte [RELEASE.md](RELEASE.md).

---

## iOS (etapa futura — exige macOS)

O código iOS (`iosMain`) já compila, mas **gerar/rodar/publicar o app iOS só funciona no macOS**
com Xcode. Quando houver um Mac disponível:

1. Criar o projeto Xcode `iosApp/` que embarca o framework `ComposeApp` (o assistente KMP do
   Android Studio gera isso, ou cria-se manualmente).
2. Configurar o **CocoaPods** para linkar os SDKs nativos do Firebase usados pelo GitLive.
3. Adicionar o **`GoogleService-Info.plist`** (app iOS no mesmo projeto Firebase).
4. Abrir `iosApp.xcworkspace` no Xcode e rodar no simulador/dispositivo.

Alternativa sem Mac local: **CI na nuvem** (GitHub Actions com runner `macos-latest` ou Codemagic).

---

## Convenções

- Imports sempre da classe final (não caminhos completos).
- Toda regra de negócio fica no backend (Firestore Rules / Cloud Functions).
- Sem código morto: dependências e símbolos não usados são removidos.
