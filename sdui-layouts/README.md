# Layouts Server-Driven UI

Estes JSON são a fonte canônica dos layouts server-driven do app, espelhando o que vive
no Firestore. O app **não** os lê daqui — ele lê do Firestore (`appScreens/{id}` e `appConfig/main`,
cada doc com um campo `json` = conteúdo do arquivo). Mantemos uma cópia versionada para histórico
e re-seed; o painel de edição no web app (etapa futura) substituirá o seed manual.

## Estrutura

- `appConfig.json` → `appConfig/main`: abas/navegação do shell (label, ícone, rota).
  - rota `"dynamic:<id>"` abre `appScreens/<id>`; rotas nativas: `notifications`, `profile`, etc.
- `home.json`, `events.json`, `devotionals.json`, `blog.json`, `projects.json`,
  `leadership.json`, `lives.json`, `forum.json` → `appScreens/<id>`.

## Componentes suportados (catálogo do app)

`lazyColumn`, `column`, `row`, `box`, `card`, `text`, `image`, `button`, `sectionHeader`,
`list`, `spacer`, `divider`. Tipos fora do catálogo são ignorados (forward-compat).

- **Texto/props**: `text` aceita bindings `{{user.firstName}}`, `{{user.displayName}}`,
  `{{user.email}}` e, dentro de um `list`, `{{item.<campo>}}`. `style` mapeia tipografia Material3.
- **`list`**: `props.source` (events, devotionals, blog, projects, lives, leaders, forumTopics,
  notifications, birthdays), `limit`, `emptyText`; `itemTemplate` renderizado por item.
- **Ações** (`action`): `navigate` (`screen` = rota ou `dynamic:<id>`; `param` = ex. `{{item.id}}`),
  `openUrl` (só http/https), `back`.

## Re-seed

```sh
# da raiz do repo web (church-management), onde há serviceAccountKey.json e node_modules:
node mobile/sdui-layouts/seed.js
# ou apontando a credencial explicitamente:
GOOGLE_APPLICATION_CREDENTIALS=/caminho/serviceAccountKey.json node mobile/sdui-layouts/seed.js
```

A coleção exige escrita de admin (ver `firestore.rules`); o `serviceAccountKey.json` ignora as rules.
