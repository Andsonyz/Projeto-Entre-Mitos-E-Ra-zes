# Revisão de estrutura e execução

Data: **21/10/2026**. Ambiente: Windows, JDK 27, JavaFX 25.0.1 e 27.

O objetivo foi executar três rodadas de análise, melhoria e validação, preservando o fluxo do jogo e a configuração de vídeos por capítulo.

## Rodadas

| Rodada | Execução | Resultado |
| --- | --- | --- |
| 1 — Diagnóstico | Suíte original de cinemáticas com JavaFX 27. | Reprodução das duas cenas e recuperação de falhas aprovadas. A análise mostrou que os testes ainda não cobriam a campanha. |
| 2 — Estrutura e integração | Testes de cinemáticas e fluxo real de `Game` com JavaFX 27. | Campanha, entradas, saves e renderização aprovados após as correções e os ajustes de preparação dos testes. |
| 3 — Compatibilidade | Suíte completa com JavaFX 25.0.1, compilação `--release 25` e nova execução completa com JavaFX 27. | Suítes aprovadas nas duas configurações. A execução do Java 25 permanece não verificada; os testes executaram no JDK 27. |

## Melhorias realizadas

- **Desenho do mundo:** removidas as implementações antigas e sem chamadas em `Game`. `WorldRenderer` concentra o desenho, e a posição dos itens é compartilhada com a lógica de colisão.
- **Seleção de telas:** substituída a sequência extensa de condições por um `switch` que cobre todos os estados. A confirmação e os avisos possuem um único caminho de desenho, com liberação do contexto gráfico em `finally`.
- **Início de fases:** nova campanha, avanço e reinício usam `resetPhaseProgress()`. Velocidade, teclas pressionadas e câmera são limpas em todas essas transições.
- **Quiz:** as quatro setas passam a selecionar alternativas, conforme as instruções exibidas na tela. Números, mouse e confirmação continuam disponíveis.
- **Saves:** coordenadas `NaN` e infinitas recebem valores padrão ao carregar. `SaveManager` permite definir um diretório próprio, preservando o caminho padrão do jogador.
- **Verificação:** compilação e seleção das suítes foram centralizadas em `scripts/test-project.ps1`. O comando anterior `test-cinematics.ps1` continua disponível e delega a compilação ao mesmo script.

## Cobertura comprovada

`CinematicRegressionTest` usa o decoder real do JavaFX para verificar as duas cenas até o fim, ordenação de sequências, legendas UTF-8, avanço, cancelamento, substituição, eventos antigos e arquivos ausentes ou corrompidos.

`GameFlowRegressionTest` cria `Game` com um catálogo de teste e um diretório temporário de saves. O teste percorre o menu e a criação de campanha, verifica a ordem entre prólogo e capítulos, libera os objetivos das quatro fases, passa pelos diálogos e responde aos quizzes. Também verifica resposta incorreta, controles por setas, bloqueio de fases, continuação sem repetir vídeos, reinício com vídeo, conclusão e isolamento dos slots.

Todos os estados de `GameState` são renderizados. Amostras de menu, mundo, quiz e confirmação são gravadas em `out/project-tests/artifacts/`; essas quatro imagens foram inspecionadas visualmente.

Os testes não abrem uma janela e não modificam os saves do jogador. Eles exercitam o código de entrada programaticamente e renderizam fora da tela; não substituem uma partida manual nem uma medição de desempenho. O teste de recuperação emite avisos de arquivos ausentes e corrompidos de propósito. Falhas nativas intermitentes também podem aparecer no terminal antes de a tentativa automática recuperar a reprodução.

## Reproduzir

Na raiz do projeto, com JDK 27:

```powershell
# Suíte completa com JavaFX 27
.\scripts\test-project.ps1

# Suíte completa com JavaFX 25.0.1 e classes destinadas ao Java 25
.\scripts\test-project.ps1 -JavaFxPath 'lib/javafx' -Release 25

# Apenas cinemáticas, mantendo o comando anterior
.\scripts\test-cinematics.ps1
```
