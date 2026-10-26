# Changelog

## Não lançado

- Exclusão da própria conta em **Perfil → Excluir minha conta**: explica o que é apagado, anonimizado e mantido, exige a senha e a palavra `EXCLUIR`, renova o token e chama o callable `deleteOwnAccount`; depois encerra a sessão, remove o token FCM do aparelho e mostra "Sua conta foi excluída." no login. Contas sem senha (só Google) são direcionadas para `/excluir-conta` no site.
- "Política de Privacidade" no Perfil, na tela de exclusão e no login (abre `/privacidade` do site). `docs/privacidade.html` alinhada à política web (LGPD, 25/10/2026), com o e-mail oficial `igreja.ibc.cacote@gmail.com` (também oferecido na tela de exclusão para ajuda ou pedido de exclusão).

## 1.0.8 (10) — 2026-10-24

- Atualizados `compileSdk` e `targetSdk` de 35 para 36 para atender ao bloqueio informado pelo Play Console.
- Atualizadas as ferramentas de compilação para AGP 8.10.1, Gradle 8.14 e Kotlin/Compose Compiler 2.2.10, preservando as bibliotecas e funcionalidades do app.
- Incrementado o código para 10 porque o pacote de código 9 já foi enviado à loja.
- A minificação continua desativada: não há arquivo de mapeamento de desofuscação a enviar. O aviso de símbolos das bibliotecas nativas é independente da correção de API.
- Validação: cinco testes unitários e três testes instrumentados no Android 16 passaram; lint sem erros, APK/AAB assinados com API 36 e código 10 conferidos no manifesto. Alinhamento de 16 KB do APK e das bibliotecas de 64 bits verificado. A aceitação final no Play Console depende do novo envio.

## 1.0.7 (9) — 2026-10-24

- Corrigida a numeração do pacote pedagógico: a faixa de teste interno já utiliza 1.0.6 com versionCode 8, conforme evidência do Play Console fornecida pelo usuário.
- Substitui os artefatos locais 1.0.4 (5) por APK/AAB 1.0.7 (9), preservando a implementação pedagógica integrada.
- O nome da versão exibido no Play Console pode ser personalizado; o controle do pacote utiliza `versionCode`, que deve crescer a cada envio.

## 1.0.4 (5) — 2026-10-21

- Coordenação Pedagógica nativa no Perfil de usuários autorizados, usando as mesmas permissões do web.
- Abas distintas de Avaliação, Aplicação e Relatórios para Igreja e ONG.
- Registro de avaliações com instrumento do servidor, vínculo à chamada, observações e encaminhamentos. Tentativas repetidas não duplicam o registro confirmado pelo backend.
- Consulta individual e coletiva com IDG, IDE e IDD calculados no servidor, filtros com espera de 350 ms e histórico completo sob demanda.
- Exportação Android em PDF paginado e Word (.docx), com seleção nativa para salvar ou compartilhar.
- Testes de contratos, idempotência, DOCX, navegação entre abas, acesso somente de leitura, carregamento sob demanda e PDF com várias páginas.

Validação: cinco testes unitários e três testes instrumentados em Android 14. Testes de tela usam dados controlados; o backend é validado separadamente com emuladores Firebase. Login e operação com contas reais em produção ainda requerem validação no aparelho. Lint sem erros; avisos existentes de atualização de dependências e de plugins permanecem.

A geração desta versão produz APK e AAB assinados. Não equivale à publicação ou aprovação na Google Play. iOS exige macOS/Xcode e não faz parte dos binários desta versão.
