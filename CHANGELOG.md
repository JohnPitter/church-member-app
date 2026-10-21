# Changelog

## 1.0.4 (5) — 2026-10-21

- Coordenação Pedagógica nativa no Perfil de usuários autorizados, usando as mesmas permissões do web.
- Abas distintas de Avaliação, Aplicação e Relatórios para Igreja e ONG.
- Registro de avaliações com instrumento do servidor, vínculo à chamada, observações e encaminhamentos. Tentativas repetidas não duplicam o registro confirmado pelo backend.
- Consulta individual e coletiva com IDG, IDE e IDD calculados no servidor, filtros com espera de 350 ms e histórico completo sob demanda.
- Exportação Android em PDF paginado e Word (.docx), com seleção nativa para salvar ou compartilhar.
- Testes de contratos, idempotência, DOCX, navegação entre abas, acesso somente de leitura, carregamento sob demanda e PDF com várias páginas.

Validação: cinco testes unitários e três testes instrumentados em Android 14. Testes de tela usam dados controlados; o backend é validado separadamente com emuladores Firebase. Login e operação com contas reais em produção ainda requerem validação no aparelho. Lint sem erros; avisos existentes de atualização de dependências e de plugins permanecem.

A geração desta versão produz APK e AAB assinados. Não equivale à publicação ou aprovação na Google Play. iOS exige macOS/Xcode e não faz parte dos binários desta versão.
