# Validação de vídeo e personagens — 21/10/2026

Alterações feitas no projeto aberto `Projeto-Entre-Mitos-E-Ra-zes`, preservando o trabalho existente no checkout. Não houve integração ao projeto MitosERaizes, commit automático ou nova dependência de execução.

## Correções

- A fase só recebe sua apresentação depois do término natural de todas as cenas configuradas. A preparação e a reprodução mantêm o mundo suspenso. O frame final permanece anexado até preparar a próxima tela.
- O player distingue carregamento, reprodução, falha, cancelamento e conclusão. Erro, timeout, clique ou tecla não liberam a fase. Cada sessão e cena tem identificador para descartar callbacks repetidos ou antigos.
- O save registra vídeos pendentes. Cancelar, falhar, continuar ou reiniciar capítulo não dispensa um prólogo ainda não concluído. Os indicadores são limpos no callback natural, ou imediatamente quando não existem cenas configuradas. Saves antigos sem os campos mantêm a retomada legada.
- Novo jogo passa pela seleção de personagem. O masculino é o padrão; o feminino está bloqueado por teclado, mouse e confirmação interna, sem imagens substitutas.
- Os quatro PNGs do ZIP fornecido substituem os sprites ativos de Greg. Recorte por faixas transparentes, escala proporcional e âncora comum nos pés preservam posição e hitbox de 42 × 58. Animações usam tempo, com envelope de desenho de 58 × 78.

Origem, dimensões, organização e SHA-256 dos recursos estão em [PERSONAGENS.md](PERSONAGENS.md). Nenhum script ou programa do ZIP foi executado. Não foi criado outro sistema de saves.

## Arquivos desta atualização

| Grupo | Arquivos |
| --- | --- |
| Vídeo | `src/main/java/br/com/raizes/audio/CinematicManager.java` |
| Fluxo, seleção e save | `src/main/java/br/com/raizes/core/Game.java`, `GameState.java` |
| Desenho e medidas | `src/main/java/br/com/raizes/core/WorldRenderer.java`, `src/main/java/br/com/raizes/config/GameConfig.java` |
| Personagens | `src/main/java/br/com/raizes/character/AnimationState.java`, `CharacterDefinition.java`, `SpriteAnimationSet.java`, `PlayerAnimator.java` |
| Imagens | `Image/Personagens/AventureiroMasculino/PersonagemParado.png`, `PersonagemAndando.png`, `PersonagemCorrendo.png`, `PersonagemPulando.png` |
| Regressões | `src/test/java/br/com/raizes/audio/CinematicRegressionTest.java`, `src/test/java/br/com/raizes/character/CharacterSpriteRegressionTest.java`, `src/test/java/br/com/raizes/core/CharacterSelectionRegressionTest.java`, `GameFlowRegressionTest.java` |
| Execução dos testes | `scripts/test-project.ps1` |
| Documentação | `README.md`, `docs/VIDEOS.md`, `docs/PERSONAGENS.md`, este relatório e `docs/superpowers/plans/2026-10-21-video-personagens.md` |

Os PNGs antigos foram preservados. `Main.java`, `run.bat` e o mecanismo de gravação de `SaveManager` não precisaram de mudanças para esta atualização.

## Evidências

| Verificação | Resultado |
| --- | --- |
| `scripts/test-project.ps1` | Suíte completa aprovada com JDK 27 e JavaFX 27. |
| `scripts/test-project.ps1 -JavaFxPath lib/javafx -Release 25` | Suíte completa aprovada com JDK 27; compilação para Java 25. |
| Integridade dos quatro PNGs | SHA-256 idêntico entre ZIP e destino. |
| FFmpeg: decodificação integral dos dois MP4s | Sem erros. |
| Programa mínimo JavaFX 25, sem lógica do jogo | 12 aberturas e quatro reproduções completas aprovadas. |
| `git diff --check` | Sem erros de whitespace; apenas aviso de normalização LF/CRLF do Git. |

Ocorreram execuções anteriores com falha nativa do decoder e um timeout do teste ao observar uma cena curta; o teste agora inclui estado da tela/player no diagnóstico. A execução isolada da campanha e as execuções completas acima passaram. Os logs finais estão em `out/project-tests/javafx25.log` e `out/project-tests/javafx27.log`. Não se considera a intermitência nativa resolvida.

As regressões reproduziram os problemas antes das correções: Enter antecipava o vídeo; confirmar o nome não abria seleção; Continue dispensava vídeos cancelados. A revisão independente também identificou o desvio pelo save e o tratamento de teclado do botão Voltar; ambos receberam correções e cobertura de regressão.

Os testes verificam:

- Duas cenas reais reproduzidas até o fim, callback final único, retenção do player até a próxima tela e descarte de eventos antigos.
- Cancelamento repetido, troca de sessão, arquivos ausentes/corrompidos, tentativa de recuperação e acionamento do botão Voltar pelo teclado.
- Física, inimigos, relógio e animação suspensos durante vídeo, com a fila de eventos do Swing responsiva.
- Seleção masculina, bloqueio feminino por três caminhos e saves sem personagem, com ID desconhecido ou indisponível.
- 56 poses, transparência, margem nas bordas, âncora comum, contagem de quadros, equivalência temporal a 30/120 atualizações por segundo e ausência de reinício contínuo da animação.
- Campanha de quatro capítulos, objetivos, quiz, reinício, saves temporários e renderização das telas.

Saves reais do usuário não foram usados nos testes. Os artefatos ficam em `out/project-tests/artifacts/`, incluindo `sprites-contact.png`, `character-male.png`, `character-female-locked.png`, `world.png`, `video-error.png` e `sprite-origin.json`.

## Limitações observadas

O controle nativo de aplicativos não se conectou ao ambiente Windows, mesmo após reinicializar a sessão da ferramenta. Foram inspecionadas as imagens de seleção, todos os quadros dos sprites, a fase renderizada e a recuperação de erro. Não foi observada uma partida interativa em janela; continuidade visual do último frame e movimento em tela ainda requerem essa conferência manual. Uma captura de vídeo fora de janela mostrou apenas o overlay e não serve como prova da imagem do decoder.

O decoder JavaFX apresentou erros nativos `UNKNOWN / ERROR_MEDIA_INVALID` intermitentes. Os mesmos arquivos passaram pela decodificação completa do FFmpeg sem erros e por um programa mínimo com JavaFX 25: 12 aberturas consecutivas e quatro reproduções naturais completas. Isso não determinou a causa da intermitência. A recuperação permanece explícita e a fase continua bloqueada se a tentativa falhar.

Não há folha independente de queda; são usadas as quatro poses finais de salto/pouso. A personagem feminina permanece indisponível e sem recursos. A execução no JDK 25 não foi testada; o ambiente de verificação utiliza JDK 27.
