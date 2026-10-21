# Vídeo e personagens — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Preserve the user's explicit instruction to implement in the currently open project.

**Goal:** Concluir os vídeos antes de liberar a fase, trocar Greg pelo aventureiro masculino e acrescentar seleção com feminino bloqueado.

**Architecture:** Manter JavaFX Media, separando término natural, cancelamento e erro. Centralizar personagens e animações em classes próprias; `Game` coordena seleção, sessão e o save existente. Recortar as folhas pelas faixas de transparência verificadas, normalizar a escala por largura de coluna e ancorar todos os quadros nos pés.

**Tech Stack:** Java 25/27, Swing/AWT, JavaFX 25.0.1/27; sem novas dependências de execução.

**Spec:** Pedido detalhado do usuário nesta sessão, seções 1–5. Projeto confirmado: `C:/Users/anton/ProjetoRaizes/Projeto-Entre-Mitos-E-Ra-zes`; ZIP confirmado: `C:/Users/anton/TestesRaizes/testesJAVA/testes.zip`.

## Global Constraints

- Alterar este checkout, preservando alterações pré-existentes; não integrar a outro repositório nem executar conteúdo do ZIP.
- Preservar hitbox existente de 42×58 e envelope visual de 58×78; este não é o protótipo 40×80/64×96.
- Vídeo: somente `onEndOfMedia` de todas as cenas libera a introdução da fase. Esc cancela; falhas exigem tentar novamente ou voltar.
- Física, relógio de fase e animações permanecem suspensos fora de `WORLD`.
- Masculino habilitado; feminino bloqueado em mouse, teclado e chamadas internas. Save sem `character` carrega masculino.
- Arquivos originais de Greg permanecem no disco, fora do fluxo ativo.

## Review Focus

- Clique/Enter/Espaço, falha ou timeout não podem ser interpretados como conclusão.
- Cancelar ou substituir uma sessão invalida callbacks antigos, inclusive os já enfileirados no Swing.
- As folhas 1254×1254 não são divisíveis por quatro e as linhas atravessam células regulares.
- Pulo 1536×1024 possui oito poses em duas linhas irregulares; subida usa as quatro primeiras e queda as quatro últimas.
- IDs ausentes, desconhecidos ou indisponíveis nos saves devem adotar masculino sem carregar recursos femininos.

### Task 1: Transição natural e recuperação

- [x] Testes RED: clique/teclas não adiantam vídeo; falha não conclui; cancelamento e callbacks repetidos não liberam fase.
- [x] `CinematicManager`: estado explícito, carregamento assíncrono, overlay de carregamento/erro, `retry()` e `cancel()`; manter último frame até o callback preparar a próxima tela.
- [x] `Game`: remover atalhos de avanço, invalidar sessão ao sair e impedir atualização da fase em cinemática.
- [x] GREEN: testes do player com decoder real e gate de fase.

### Task 2: Personagem e sprites

- [x] Testes RED: catálogo bloqueia feminino; animação usa tempo; quadros e âncoras permanecem consistentes.
- [x] Copiar somente os quatro PNGs inspecionados para nova pasta; registrar dimensões, faixas, hash e origem.
- [x] `CharacterDefinition`, `AnimationState`, `SpriteAnimationSet` e `PlayerAnimator`: recorte, escala uniforme, cache e desenho com âncora compartilhada.
- [x] `Game`/`WorldRenderer`: usar o animador sem alterar hitbox; A/D mantém corrida e Shift permite andar.
- [x] GREEN: testes de contagem de quadros, ausência de corte e sequência temporal; imagens de contato para inspeção.

### Task 3: Seleção e save

- [x] Testes RED: nome → seleção; masculino confirma; feminino bloqueado por três caminhos; voltar preserva nome; saves legados normalizam personagem.
- [x] Tela `CHARACTER_SELECT` com cartões no estilo existente, masculino selecionado e feminino “Em breve” sem sprite substituto.
- [x] Persistir `character` no mesmo arquivo de save. Carregar recursos somente do personagem habilitado.
- [x] GREEN: adaptar teste da campanha para aguardar fim natural e verificar suspensão durante vídeo.

### Task 4: Verificação e documentação

- [x] Executar `scripts/test-project.ps1` e a variante JavaFX 25.0.1 com `-Release 25`.
- [x] Executar/observar o jogo quando o ambiente permitir; inspecionar seleção, movimento, salto, queda e imagens geradas.
- [x] Revisão independente e correções necessárias com regressões.
- [x] Atualizar README/guia e relatório com arquivos, ZIP, resultados e limitações reais.

## Progress

- ZIP extraído em diretório temporário validado; somente imagens serão copiadas. `testes.java` foi lido, não executado.
- Baseline anterior às alterações: suíte completa JavaFX 27 aprovada; confirmado que o comportamento antigo permite pular vídeos e continuar após falhas, contrariando este pedido.
- A execução é feita diretamente no checkout solicitado pelo usuário; nenhuma integração, mudança de branch ou commit automático será realizado.

- Implementação concluída: nenhum avanço manual ou erro chama a conclusão; fase e relógio ficam suspensos fora de WORLD. Os quatro PNGs foram copiados e conferidos por SHA-256.
- RED observado para Enter antecipando vídeo, ausência de seleção, cancelamento duplicado e Continue dispensando vídeo pendente; os casos corrigidos passaram nas regressões.
- Revisão independente: corrigidos os indicadores persistidos pendingPrologue/pendingChapter e o teclado do botão Voltar. Reinício de capítulo também preserva prólogo pendente.
- Suítes completas finais aprovadas com JavaFX 27 e JavaFX 25.0.1 / JDK 27 / --release 25. Resultados consolidados em docs/VALIDACAO-VIDEO-PERSONAGENS.md; logs em out/project-tests/javafx25.log e javafx27.log.
- Verificação visual: folhas originais, contato de 56 poses, cartões, fase e tela de erro inspecionados. Controle nativo do Windows indisponível mesmo após reinicializar a ferramenta; observação interativa da janela permanece pendente.
- Decoder apresentou UNKNOWN intermitente. FFmpeg decodificou os dois arquivos sem erro; programa mínimo JavaFX 25 passou 12 aberturas e quatro reproduções completas. Não foi aplicada mudança especulativa de codec, biblioteca ou renderizador.
