# Diagnóstico técnico do jogo — 20/11/2026

> Registro cronológico. A atualização mais recente de jogabilidade, plataformas, menu e celebração está na **Etapa 4**, ao final. Os estados das etapas anteriores registram o que foi observado em cada rodada.

**Estado:** parcialmente validado. O projeto compila pelo comando de testes, mas a execução documentada por `run.bat` falha. A partida manual também para no prólogo porque os caminhos de vídeo configurados não existem. Esta etapa foi somente de inspeção e registro: nenhum código, asset, save real, configuração ou dependência do jogo foi alterado.

## Projeto e ambiente

- Raiz verificada: `C:\Users\anton\ProjetoRaizes\Projeto-Entre-Mitos-E-Ra-zes` (raiz Git do checkout aberto no VS Code). É o jogo principal, com `src/main/java/br/com/raizes/Main.java` como ponto de entrada; não é o protótipo separado.
- Não foi encontrado `AGENTS.md` na raiz nem nos diretórios pais consultados. Este registro não existia antes.
- Ambiente: Windows 11 Home Single Language, build 26300; JDK/JRE e `javac` 27; JavaFX 27 para os testes e para a abertura manual temporária. Tela observada: 1536 × 864, jogo em tela cheia.
- `README.md` orienta `.\run.bat` (JavaFX 25.0.1) ou a configuração `Entre Mitos e Raizes - JavaFX 27` no VS Code. O `run.bat` compila fontes com `javac @out\sources.txt`, copia recursos para `out\` e inicia `br.com.raizes.Main`. O script de testes é `scripts/test-project.ps1`; compila fontes principais e de teste para `out/project-tests/classes`.
- Saves são arquivos Java Properties (`slot-1.properties` e `slot-2.properties`) em `%USERPROFILE%\.entremitoseraizes\saves`, pelo `SaveManager`. A execução manual desta inspeção usou `-Duser.home` apontando para `out/diagnostic-home`; o save real não foi aberto nem modificado.

## Recursos encontrados e ausências

- Encontrados: `Image/Cenarios/Menu/MENU.jpg`, `Titulo.png`, botões do menu, cartões de slots em `src/main/resources/Image/Cenarios/Menu/Slots/`, telas de pausa/configuração em `src/main/resources/Image/Cenarios/Menu/`, quatro fundos de fase em `Image/Cenarios/BackGrounds/` e imagens de elementos em `Image/Cenarios/Elementos/`.
- Encontrados cinco vídeos: `Videos/Cenas/CenaFase1.mp4`, `CenaFase2.mp4`, `CenaFase3.mp4`, `CenaFase4.mp4` e `CenaFinal.mp4`.
- Ausentes neste checkout: diretórios `AudioJOGO/`, `IMAGENS_SVG/` e `Backgrounds/`. O diretório real de fundos é `Image/Cenarios/BackGrounds/` (essa capitalização e localização). A busca por nomes `capa`, `cover`, `load`, `carreg` e `loading` não encontrou arquivo dedicado de nova capa ou tela de carregamento; há arte de menu e cartões de slot com outros nomes.
- `Videos/cinematics.properties` configura `Videos/cena1.mp4` e `Videos/cena2.mp4`, que não existem. Os vídeos presentes em `Videos/Cenas/` têm outros nomes e não são referenciados pelo prólogo atual. A tentativa de criar uma campanha confirma que o jogo pede `cena1.mp4`.
- A execução registrou avisos de ausência para `Image/Cenarios/Elementos/arvore01.png`, `arvore02.png` e `arvore03.png`. São arquivos ausentes neste checkout, não incluídos na contagem de recursos encontrados.

## Fluxo e resultado do percurso manual

A janela abriu no menu. Foi possível acessar slots, a etapa de nome e a seleção de personagem; masculino apareceu disponível e selecionado, feminino bloqueado como “Em breve”. Ao confirmar, o fluxo entrou na tela de prólogo, mas a cena configurada não pôde ser reproduzida. A tela apresentou mensagem de falha com ações “Tentar novamente” e “Voltar”; “Voltar” retornou à seleção de personagem. Isso confirma a falha e a recuperação de UI, não o término natural do vídeo.

O roteiro solicitado — abrir → menu → iniciar fase → assistir à cena → caminhar → questionário → mapa → repetir entrada e saída — ficou bloqueado na reprodução da primeira cena. Não foi possível alcançar fase, caminhada, questionário ou mapa, nem observar transições repetidas. A tela do vídeo permaneceu responsiva o suficiente para mostrar a falha e aceitar “Voltar”.

## Compilação e verificações

| Verificação | Resultado observado |
| --- | --- |
| `.\scripts\test-project.ps1 -JavaFxPath 'lib/javafx-sdk-27/lib'` | A compilação de fontes e testes terminou sem erro. `CharacterSpriteRegressionTest` passou (56 poses). `CharacterSelectionRegressionTest` passou (inclui bloqueio feminino e compatibilidade de saves antigos), emitindo avisos esperados de recursos/vídeos ausentes. `CinematicRegressionTest` falhou: `Videos/cena1.mp4` não encontrado; `estado=FAILED`. O script interrompeu a suíte nesse ponto. |
| `GameFlowRegressionTest` executado diretamente, após a compilação acima | Falhou ao iniciar o vídeo de capítulo, com `screen=CINEMATIC`, `playback=FAILED`, `player=null`, pela mesma ausência de `Videos/cena1.mp4`. |
| `cmd /c .\run.bat` com `JAVA_TOOL_OPTIONS=-Duser.home=...\out\diagnostic-home` | Falhou antes de abrir o jogo. `javac` reportou `Invalid filename` e recebeu caminho semelhante a `C:Usersanton...srcmainjava...Main.java`, sem barras. O script grava caminhos entre aspas em `out\sources.txt`, mas o leitor de argumentos do `javac` não recebeu caminhos válidos nesse ambiente. |
| Compilação e abertura manual temporária de `br.com.raizes.Main` com JavaFX 27 | Passou; abriu a janela e permitiu o percurso até a tela de falha do prólogo. Recursos foram copiados somente para `out/project-tests/classes`, ignorado pelo Git. Save isolado em `out/diagnostic-home`. |
| Regressão de sprites/seleção e saves antigos | Passou nos dois testes indicados acima. |
| JDK 25 em runtime, duração de abertura/entrada, p95 de frame, frequência de picos, memória e número de players em transições repetidas | Não medidos nesta rodada. O percurso foi bloqueado antes da fase. A constante de `Timer` de 16 ms no código é o intervalo solicitado ao Swing, não uma medição de duração real de quadro. `CinematicManager` mantém um campo de `MediaPlayer` e descarta o anterior ao substituir; isso descreve o código, não uma contagem de instâncias observada em runtime. |
| `git diff --check` e comparação final com estado inicial | Estado inicial limpo; `git status` final mostrou apenas este arquivo novo. `git diff --no-index --check /dev/null docs/atualizacao-jogo.md` e verificação de espaços finais passaram após remover uma linha vazia extra. Arquivos de compilação e execução estão sob `out/`, ignorado pelo Git. |

## Reprodução no Windows

Na raiz do projeto, com JDK 27:

1. Execute `.\scripts\test-project.ps1 -JavaFxPath 'lib/javafx-sdk-27/lib'`. A compilação deve terminar, os testes de sprite e seleção devem passar e o teste de cinema deve falhar no caminho ausente `Videos/cena1.mp4`.
2. Para observar a UI sem tocar nos saves do perfil, use a saída já compilada pelo script, copie os recursos para a saída e inicie com perfil temporário:

```powershell
$classes = 'out/project-tests/classes'
Copy-Item Image $classes -Recurse -Force
Copy-Item src/main/resources/Image "$classes/Image" -Recurse -Force
Copy-Item Videos $classes -Recurse -Force
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$PWD/out/diagnostic-home"
java --module-path 'lib/javafx-sdk-27/lib' --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -cp $classes br.com.raizes.Main
```

3. Na janela: Novo jogo → slot vazio → nome → confirmar masculino → confirmar seleção. Resultado atual esperado: a cena `Videos/cena1.mp4` falha porque o caminho não existe; “Voltar” retorna à seleção. Interrompa aí. Para completar o restante do roteiro de desempenho, primeiro será necessário haver um caminho de vídeo válido e executar novamente a campanha; isso é trabalho de outra etapa e não foi aplicado aqui.

## Melhorias sugeridas (sem implementação nesta etapa)

1. **Prioridade alta — reconciliar o catálogo de vídeo e os arquivos existentes.** Problema observado: configuração e testes pedem `Videos/cena1.mp4`/`cena2.mp4`, enquanto os cinco MP4s estão em `Videos/Cenas/` com outros nomes. Benefício: permitir prólogo, fase e medições de transição. Esforço/risco: baixo a médio, exige confirmar qual conteúdo corresponde às duas cenas do prólogo antes de alterar caminhos. Teste: rodar os testes de cinema e fluxo completos e reproduzir naturalmente a cena.
2. **Prioridade alta — corrigir a lista de fontes usada pelo `run.bat`.** Problema observado: `javac @sources.txt` rejeita caminho Windows mal interpretado. Benefício: execução documentada pelo script funcionar sem compilação manual auxiliar. Esforço/risco: baixo, requer validar espaços/acentos nos caminhos. Teste: executar `run.bat` numa raiz com espaços, confirmar compilação, cópia de assets e abertura do menu.
3. **Prioridade média — localizar ou restaurar as três imagens de árvore.** Problema observado: carregador registra ausência de `arvore01.png`, `arvore02.png` e `arvore03.png`. Benefício: confirmar que a composição do cenário está completa. Esforço/risco: baixo após localizar a origem correta; não substituir por imagens inventadas. Teste: conferir carregamento sem avisos e comparar a tela de fase.

**Conclusão desta etapa:** diagnóstico reproduzível registrado, mas parcialmente validado. O roteiro de jogo e as métricas de performance permanecem pendentes porque o prólogo não encontra o vídeo configurado. Nenhuma correção foi aplicada para preservar o comportamento e respeitar o escopo somente diagnóstico/documentação.

## Etapa 2 — redução de trabalho no renderizador (20/11/2026)

**Estado: parcialmente validado.** O JFR localizou trabalho evitável ao renderizar objetivos do mundo: os cinco tipos de arte eram redimensionados pelo `drawImage` durante cada quadro, e o renderizador percorria e submetia à pintura entidades fora da câmera. O registro da etapa 1 não continha tempos de quadro nem um percurso medido; portanto, não há comparação direta com uma referência quantitativa daquele dia.

### Alteração aplicada

- `Game.loadImages()` recorta e ajusta uma vez as artes de sementes, lixo, armadilhas, lago e fogo às caixas máximas de desenho já usadas pelo mundo. O renderizador passa a desenhá-las no tamanho preparado, preservando proporção e posição. A regressão verifica que cada imagem permanece dentro da caixa e contém pixels visíveis.
- `WorldRenderer` ignora árvores, plataformas, armadilhas, fogo, objetivos, inimigos e fontes de água que não intersectam a faixa horizontal visível. Os objetos, coordenadas, colisões, física e simulação permanecem iguais. A imagem do mundo renderizada antes e depois do recorte ficou idêntica pixel a pixel em câmera `X=0` na verificação local.
- Uma comparação adicional contra o renderizador sem recorte confirmou 641 casos idênticos pixel a pixel nas quatro fases, a 1536×864, incluindo câmeras fracionárias junto às bordas de plataformas, objetivos, armadilhas, fogo e inimigos. Ela isola o recorte da câmera usando as mesmas artes preparadas em ambos os renderizadores; não é uma comparação de todos os pixels contra o redimensionamento antigo das artes em cada quadro.
- O conjunto de arte preparado substitui os recortes originais grandes para esses cinco usos; não foi criado cache de imagens de todas as fases.

### Medição comparável antes/depois

Ambas as medições usaram Windows 11, JDK 27, JavaFX 27, tela lógica 1280×720 desenhada numa superfície 1536×864, câmera inicial `X=0`, 1000 quadros de aquecimento e 600 amostras de `paintComponent` na EDT. O teste é um benchmark isolado de pintura; não representa p95 de uma partida interativa. As gravações JFR usaram `settings=profile`, com 30 segundos cada.

| Métrica | Código anterior | Código atualizado | Diferença observada |
| --- | ---: | ---: | ---: |
| P50 de pintura | 6,99 ms | 7,00 ms | praticamente igual |
| P95 de pintura | 9,31 ms | 9,14 ms | −0,18 ms (−1,9%) |
| P99 de pintura | 10,99 ms | 10,92 ms | −0,06 ms |
| Máximo da amostra | 13,50 ms | 12,53 ms | −0,97 ms |
| Pressão de alocação em `DrawImage.renderImageXform` | 1,31% | 0,29% | −78% relativo |
| Pausas de GC na gravação inteira | 56 ms / 29 pausas | 79 ms / 37 pausas | pior na amostra atualizada |

O ganho comprovado é a menor pressão de alocação associada às transformações das imagens; a latência de pintura aquecida mudou pouco. As pausas de GC incluem inicialização, JavaFX e renderização, e a diferença entre duas gravações não permite atribuir sua piora somente a esta alteração. Nenhum FPS foi reduzido.

### Fluxo e recursos de vídeo

A interface foi aberta em 1536×864 com save isolado em `out/diagnostic-home`. O vídeo da fase 1 terminou naturalmente, abriu a introdução da fase e, ao confirmar, a exploração apareceu com o personagem. Uma captura durante a cena mostrou tela preta por alguns segundos enquanto o vídeo era preparado; depois a reprodução apareceu. A medição de memória do processo ficou em aproximadamente 450 MiB residente durante a entrada e 360 MiB na fase; o JFR de outro trecho reportou pico de 597 MiB. Isso cobre uma transição, não demonstra ausência de acúmulo em reentradas repetidas.

O fluxo de campanha automatizado falhou ao iniciar `CenaFase2.mp4`: duas tentativas do JavaFX/GStreamer terminaram em `ERROR_MEDIA_INVALID`, e o gerenciador deixou a tela em `FAILED`, sem iniciar a fase seguinte. O catálogo e os arquivos confirmam que a cena existe. A inspeção MP4 encontrou H.264 Main na cena 1 e H.264 High nas cenas 2–4, todos com AAC-LC estéreo a 48 kHz. Essa diferença é uma hipótese a investigar, não uma causa comprovada. Remover o áudio descartaria conteúdo, por isso foi preservado. Para diagnóstico isolado, foi obtido FFmpeg portátil (imageio-ffmpeg 0.8.1, hash conferido com a publicação do PyPI) sob `out/diagnostic-tools`, sem instalação ou alteração de dependências do jogo. Cópias temporárias das cenas 2–4 foram recodificadas em H.264 Main, mantendo AAC por cópia direta; os MP4s originais não foram alterados. A cópia da cena 2 concluiu duas reproduções naturais isoladas e o gerenciador liberou o player em ambas. Isso reforça a hipótese de compatibilidade do perfil, mas não prova que ela seja a única causa da falha da campanha.

### Verificações

| Verificação | Resultado |
| --- | --- |
| Regressão de sprites (`CharacterSpriteRegressionTest`) | Passou: 56 poses, transparência, âncora e relógio de animação. |
| Regressão de seleção e save (`CharacterSelectionRegressionTest`) | Passou: bloqueio feminino e compatibilidade com saves antigos; avisos esperados de árvore e vídeo ausentes. |
| Nova regressão de renderizador (`WorldRendererRegressionTest`) | Passou: artes preparadas permanecem visíveis e dentro das caixas de desenho. |
| Comparação de quadro antes/depois do recorte da câmera | Passou: 0 pixels diferentes em 1536×864 para a câmera inicial. |
| Cinemáticas (`CinematicRegressionTest`) | Passou: cenas naturais, término único, cancelamento, erro e recuperação; a cena 2 exigiu retry do decoder. |
| Fluxo completo (`GameFlowRegressionTest`) com arquivos originais | Falhou ao iniciar `CenaFase2.mp4`; percurso parou antes de completar as fases e testar reentrada. |
| Fluxo completo com cópias H.264 Main, `-Xmx512m -XX:ActiveProcessorCount=4` | Não validado: o teste imprimiu `OutOfMemoryError` durante execução/encerramento e excedeu o limite de 5 s para liberar o JavaFX. Os logs não demonstram se a causa foi limite de memória do host, consumo do decoder ou acumulação do jogo. |
| Reprodução isolada da cena 2 convertida, duas sessões sequenciais no mesmo `CinematicManager` | Passou: término natural em ambas, sem erros e campo de player liberado após cada sessão. Não mede estabilidade da campanha completa nem memória residente. |
| Compilação JDK 27/JavaFX 27 | Passou durante a compilação da suíte e dos testes focados. |
| Compatibilidade de compilação JDK 25/JavaFX 25.0.1 | Passou com `javac --release 25` sobre todas as fontes principais. |
| Percurso visual até a fase 1 | Passou até a exploração; não completou coleta, quiz, mapa e reentrada. |
| Comparação quantitativa com a etapa 1, renderização de fases 2–4 e contagem de `MediaPlayer` em reentradas | Não executada/conclusiva: a etapa 1 não mediu quadros e a transição para a cena 2 falhou. |

Os avisos de `arvore01.png`, `arvore02.png` e `arvore03.png` continuam porque esses arquivos não estão neste checkout; permanecem fora da otimização e da medição visual.

### Investigação adicional da falta de memória

O log `out/diagnostic-tools/gameflow-stage/hs_err_pid18756.log` distingue heap de memória nativa: o heap usado após GC estava próximo de 90 MiB, mas o processo chegou a 3787 MiB de memória privada, e o Windows ficou com apenas 4 MiB de limite de commit disponível. A falta de memória não pode ser tratada aumentando o heap. Nenhuma configuração global de memória foi alterada.

O percurso foi repetido com uma `JFrame` conectada ao jogo, vídeos originais e saves temporários. Ele chegou à preparação da cena 3; a memória privada passou de 644,6 MiB a 2139,8 MiB entre duas amostras separadas por 42 segundos. O processo de diagnóstico foi interrompido nesse limite, antes de outra exaustão do sistema. Isso descarta a ausência de janela como explicação suficiente. O teste não concluiu a campanha.

Foi localizada a correção oficial [JDK-8389515 no OpenJFX](https://github.com/openjdk/jfx/commit/7533c6156a76597bd39a8f935585796bbfa19ef6): buffers de vídeo decodificados no `dshowwrapper` do Windows permaneciam mapeados. O projeto contém `javafx.runtime.version=27+30`; a correção consta das [notas da versão 27.0.1](https://gluonhq.com/products/javafx/openjfx-27-release-notes/). Ela também foi portada para 25.0.5; o `run.bat` usa 25.0.1. Portanto, apenas alternar entre as duas distribuições já presentes não aplica a correção.

Para atribuir a causa, um reprodutor mínimo sem classes do jogo executou três ciclos de 8 segundos, seguidos de `dispose()` e 1,5 segundo de descanso, usando o mesmo `CenaFase1.mp4` original. JDK 27, `-Xms64m -Xmx128m`, sem mudança de taxa de reprodução, resolução ou conteúdo. As bibliotecas 27.0.1 foram baixadas do Maven Central somente para `out/diagnostic-tools/javafx27-fixed`; seus hashes SHA-1 foram conferidos com os publicados no repositório e SHA-256 foi registrado. As bibliotecas versionadas em `lib/` não foram modificadas.

| Memória privada do reprodutor mínimo | JavaFX 27+30 do projeto | JavaFX 27.0.1+5 isolado |
| --- | ---: | ---: |
| Amostra próxima de 3 segundos | 430,9 MiB | 364,5 MiB |
| Maior amostra em aproximadamente 30 segundos | 1243,4 MiB | 370,7 MiB |
| Última amostra, após os três descartes | 1243,4 MiB | 304,2 MiB |
| Players descartados | 3 de 3 | 3 de 3 |

O acúmulo foi reproduzido sem `Game`, imagens, física, saves ou renderizador do mundo, e desapareceu nesse experimento ao aplicar a versão com a correção nativa. Essa é a evidência mais forte da causa da exaustão; `MediaPlayer.dispose()` sozinho não corrige buffers que a biblioteca deixa mapeados. A hipótese anterior de perfil H.264 não explica esse vazamento, pois o vídeo Main da fase 1 também o reproduz.

A nova regressão `CinematicResourceRegressionTest` usa o gerenciador real em três ciclos de reprodução/cancelamento. Ela confere `DISPOSED`, campo de player vazio, callback de cancelamento único e ausência de conclusão indevida. No Windows, confere ainda que a memória comprometida não cresça mais de 256 MiB depois do primeiro ciclo aquecido. Esse limite folgado busca o vazamento de centenas de MiB observado; não é uma garantia de ausência de todo vazamento possível. O teste foi colocado antes das cinemáticas longas em `scripts/test-project.ps1` para falhar cedo nesse ambiente.

Resultados da regressão adicionada, executada com JDK 27 e `-Xms64m -Xmx128m`:

| Distribuição | Commit após cada ciclo | Resultado |
| --- | --- | --- |
| JavaFX 27+30 do projeto | 616,3 → 944,2 MiB | Falhou no segundo ciclo: +327 MiB depois do aquecimento. Ambos os players estavam descartados. O teste interrompeu cedo, antes de esgotar o host. |
| JavaFX 27.0.1+5 isolado | 304,7 → 310,4 → 318,2 MiB | Passou: três players descartados e +13,5 MiB após aquecimento. |

Comandos de reprodução na raiz, após compilar pelo script de testes:

```powershell
java -Xms64m -Xmx128m --module-path lib/javafx-sdk-27/lib --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -cp out/project-tests/classes br.com.raizes.audio.CinematicResourceRegressionTest
# A comparação corrigida usa somente o pacote temporário; não substitui lib/.
java -Xms64m -Xmx128m --module-path out/diagnostic-tools/javafx27-fixed --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -cp out/project-tests/classes br.com.raizes.audio.CinematicResourceRegressionTest
```

Todas as fontes principais e de teste, incluindo a nova regressão, também compilaram com `javac --release 25` e o JavaFX 25.0.1 já presente. Isso demonstra compatibilidade de compilação; não corrige nem valida o vazamento do runtime 25.0.1.

Um controle posterior executou a regressão de recursos também nas duas distribuições 25, com os mesmos parâmetros e o mesmo MP4 da fase 1:

| Distribuição 25 | Commit após cada ciclo | Resultado |
| --- | --- | --- |
| JavaFX 25.0.1 já presente em `lib/javafx/` | 304,9 → 309,8 → 311,9 MiB | Passou nos três ciclos; +7,0 MiB após aquecimento. |
| JavaFX 25.0.5 temporário em `out/diagnostic-tools/javafx25-fixed` | 305,6 → 308,9 → 315,3 MiB | Passou nos três ciclos; +9,7 MiB após aquecimento. |

O controle limita a conclusão anterior: embora a correção do OpenJFX conste da versão 25.0.5, o vazamento observado com JavaFX 27+30 não foi reproduzido com 25.0.1 nesse ensaio. Portanto, não há evidência para exigir a atualização das duas distribuições como condição da correção deste caso. A proposta foi reduzida à distribuição 27 afetada; a campanha completa com a distribuição 25.0.1 passou a ser validada separadamente. Os pacotes 25.0.5 e 27.0.1 continuam somente em `out/`, sem alterar dependências versionadas.

O teste completo posterior distinguiu o limite desse controle curto:

| Comando da suíte completa, JDK 27 e fontes com `--release 25` | Resultado observado |
| --- | --- |
| `scripts/test-project.ps1 -JavaFxPath lib/javafx -Release 25` | Compilação, sprites, seleção, renderizador e recursos passaram. `CinematicRegressionTest` falhou ao preparar a primeira cena original: duas tentativas retornaram `ERROR_MEDIA_INVALID`, estado `FAILED`. A suíte interrompeu antes de `GameFlowRegressionTest`; esse fluxo não foi executado nessa rodada. Não foi uma falha do limite de memória da nova regressão. |
| `scripts/test-project.ps1 -JavaFxPath out/diagnostic-tools/javafx25-fixed -Release 25` | Passou integralmente: compilação, 56 poses, seleção/saves antigos, renderizador, três ciclos de recursos, cinemáticas naturais/cancelamento/erro/recuperação e campanha de quatro capítulos com reentrada e saves isolados. Os vídeos foram os originais. |

Os dois runtimes candidatos passaram na campanha, mas as evidências não são idênticas: em 27+30 foi isolado o acúmulo de memória nativa; em 25.0.1 houve falha de preparação nessa rodada, enquanto os três ciclos curtos da cena 1 passaram. Uma execução bem-sucedida em 25.0.5 não prova ausência de toda falha transitória do decoder. A aprovação para atualizar o 27 não autoriza automaticamente modificar a distribuição 25; eventual atualização desse segundo caminho precisa respeitar a mesma restrição de dependências do roteiro.

Na repetição da suíte com o pacote corrigido isolado, o teste de campanha completou os quatro capítulos, mas encontrou uma expectativa antiga na última reentrada: as teclas selecionavam a fase 2 desbloqueada e a asserção esperava o vídeo da fase 1, que antes era reutilizado pelo catálogo de teste. `GameFlowRegressionTest` passou a conferir explicitamente o índice selecionado (`1`), o vídeo correspondente (`CenaFase2.mp4`) e o estado reiniciado dessa mesma fase. O comportamento de navegação do jogo não foi alterado.

Após corrigir a asserção, a execução direta de `GameFlowRegressionTest` com JavaFX 27.0.1 isolado passou, incluindo os quatro capítulos, seus términos naturais de vídeo, objetivos, quizzes, saves isolados, continuação e reinício explícito da fase 2. Os vídeos usados foram os originais de `Videos/Cenas/`, sem conversão. Houve uma tentativa automática de reinício do decoder da cena 2, recuperada pelo gerenciador existente. Portanto, a atualização corrigiu a exaustão neste percurso, mas não eliminou todo erro transitório do decoder.

No processo dessa repetição (`-Xms128m -Xmx512m`), dez amostras a cada aproximadamente 8 segundos registraram de 572,1 a 584,9 MiB de memória privada. A amostragem começou após a inicialização e durou 74 segundos, até o processo terminar; não cobre todos os instantes nem permite concluir estabilidade em uma sessão indefinidamente longa. Os dados estão em `out/diagnostic-tools/fixed-gameflow-repeat-memory.csv`. O teste anterior, sem limite explícito de heap e ainda com a asserção incorreta, manteve de 702,2 a 715,3 MiB nas amostras observadas; os valores absolutos entre esses dois processos não são uma comparação controlada de desempenho.

| Verificação final adicional | Resultado |
| --- | --- |
| `scripts/test-project.ps1 -JavaFxPath out/diagnostic-tools/javafx27-fixed` | Compilação, sprites, seleção, renderizador e cinemáticas passaram. A campanha revelou a asserção incorreta da reentrada, corrigida em seguida. Essa rodada foi iniciada antes da inclusão do novo teste de recursos no script. |
| `java -Xms128m -Xmx512m --module-path out/diagnostic-tools/javafx27-fixed --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -cp out/project-tests/classes br.com.raizes.core.GameFlowRegressionTest` | Passou após corrigir a expectativa do capítulo selecionado, sem modificar o código de navegação nem os vídeos. |
| `CinematicResourceRegressionTest` executado separadamente com JavaFX 27.0.1 | Passou; foi adicionado depois do início da rodada de suíte acima e por isso teve execução própria. |
| Comparação de recorte em quatro fases | Passou: 641 câmeras, incluindo bordas fracionárias, pixel a pixel idênticas ao desenho sem recorte com as mesmas imagens preparadas. |
| Compilação de todas as fontes e testes com `--release 25` / JavaFX 25.0.1 | Passou. A validação posterior de execução das distribuições 25 está discriminada na tabela específica acima. |
| `git diff --check`, arquivos novos e diretórios `lib/` / `Videos/Cenas/` | Passou; sem arquivos removidos ou movidos, sem alterações nas dependências ou MP4s originais. Alterações anteriores em catálogo e launcher foram preservadas. |
| Partida visual completa e comparação de FPS com etapa 1 | Não executadas: a observação manual cobre a primeira fase; a etapa 1 não registrou uma referência de tempos de quadro. Os testes automatizados não substituem a observação visual completa. |

Aplicar o pacote corrigido ao jogo requer uma exceção à instrução desta etapa de não alterar dependências. A atualização de `lib/`, ajustes de caminhos/versionamento de execução e documentação de compatibilidade ficam pendentes de autorização. O diagnóstico temporário não promoveu esses arquivos ao jogo.

O SDK completo 27.0.1 do fornecedor também foi preparado em `out/diagnostic-tools/sdk27-candidate/javafx-sdk-27.0.1`. O arquivo original foi obtido em `https://download2.gluonhq.com/openjfx/27.0.1/openjfx-27.0.1_windows-x64_bin-sdk.zip`, com SHA-256 local `FCFE5E1DD25BF7136F394D6B02EA05277D650FBB28AEE9F94F5E8209D1C7C6A9`. Suas 58 DLLs foram comparadas com as DLLs incluídas nos artefatos Maven verificados usados no teste; todas coincidiram. O manifesto `out/diagnostic-tools/sdk27-candidate-manifest.csv` registra os hashes antigos e candidatos: 27 arquivos diferem, 87 são iguais, sem adições ou remoções no conjunto do SDK. Isso prepara uma atualização coerente do SDK inteiro, evitando trocar uma DLL isoladamente ou misturar módulos. `lib/javafx-sdk-27/` continua intacto.

Uma conferência adicional dos JARs confirmou também que as 5749 classes e recursos (fora dos metadados de empacotamento em `META-INF`) dos módulos base, graphics, controls, media e swing coincidem byte a byte entre esse SDK candidato e os artefatos Maven usados nos testes. As DLLs e os módulos efetivamente exercitados nos testes são, assim, os mesmos do pacote completo preparado para revisão.

### Roteiro manual ainda pendente

O percurso visual completo deve usar perfil isolado e a mesma superfície 1536×864 da inspeção inicial. O comando abaixo usa o SDK candidato temporário e classes já compiladas; não modifica `lib/` nem os saves do usuário:

```powershell
java "-Duser.home=$PWD/out/diagnostic-home-manual" --module-path out/diagnostic-tools/sdk27-candidate/javafx-sdk-27.0.1/lib --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -cp out/project-tests/classes br.com.raizes.Main
```

1. Novo jogo → slot vazio → nome → masculino → confirmar. A cena deve carregar com UI responsiva, terminar naturalmente e só então abrir a introdução; confirmar a introdução abre a fase.
2. Durante o vídeo, tentar mover/pular. O personagem, inimigos, física e relógio da fase devem permanecer suspensos. Cancelar com Esc e repetir a entrada deve impedir que callbacks da tentativa anterior iniciem a fase.
3. Na fase, caminhar pelo mesmo trecho após aquecimento de 30 segundos, coletar os objetivos, abrir/fechar mapa e pausa, concluir o desafio do guardião e avançar. Registrar p95 e picos desse trecho; a etapa 1 não fornece números anteriores comparáveis.
4. Repetir continuar e reiniciar uma fase desbloqueada pelo menos cinco vezes, anotando memória privada após cada retorno e verificando que há no máximo um player durante vídeo e nenhum após a transição. A memória deve estabilizar após aquecimento, sem crescimento proporcional ao número de quadros assistidos.
5. Observar proporção, âncora, bordas de câmera, áudio e transições nas quatro fases. Os 641 quadros comparados e a campanha automatizada não substituem essa inspeção visual. Fechar a janela deve encerrar o processo e liberar o player.

Este roteiro não foi marcado como executado integralmente. A observação manual existente cobre somente a primeira fase; os resultados automatizados e de memória estão discriminados acima.

### Melhorias sugeridas

1. **Prioridade alta — aplicar a correção nativa do OpenJFX com autorização para atualizar a dependência afetada.** Problema observado: centenas de MiB se acumulam durante decodificação com JavaFX 27+30, mesmo com players descartados. Proposta: atualizar somente a distribuição 27 para 27.0.1, mantendo o JDK e os vídeos originais; o controle curto com 25.0.1 não reproduziu o vazamento. Benefício: evitar esgotamento de memória no caminho configurado pelo VS Code. Esforço/risco: médio; alinhar JARs, bibliotecas nativas e documentação, sem misturar versões de módulos. Teste: executar a regressão de recursos, as cinemáticas e a campanha completa; repetir reentradas e medir memória privada.

**Conclusão da etapa 2:** a redução de trabalho e alocação foi implementada e medida, com pequeno ganho de p95 no benchmark isolado. A exaustão de memória foi isolada em buffers nativos da distribuição JavaFX atual e reproduzida por uma regressão que falha cedo. A versão 27.0.1 corrigida passou nessa regressão, no reprodutor mínimo e no fluxo automatizado dos quatro capítulos com reentrada, usando os vídeos originais. Ela continua apenas em `out/`: aplicar a correção à dependência está fora da autorização atual. A etapa permanece parcialmente validada no pacote do projeto, que ainda usa a distribuição defeituosa. Não se declara validação visual completa nem equivalência quantitativa com a etapa 1, que não registrou FPS.

### Decisão posterior — manter as imagens atuais (20/11/2026)

Por solicitação do usuário, a integração das novas artes SVG foi cancelada. Os PNGs e JPGs existentes, os sprites de Greg e do personagem masculino e os fundos atualmente utilizados foram preservados. Não foi implantado conversor SVG, nova biblioteca ou troca dos fundos por fase. O teste exploratório dessa troca foi removido; os arquivos extraídos para inspeção permanecem apenas em `out/`, fora do fluxo de execução.

Verificação desta decisão: todas as fontes de produção e testes compilaram com `--release 25`, usando o JDK 27 e o JavaFX 27 já existentes no projeto. As 44 imagens PNG/JPG em `Image/` e `src/main/resources/` puderam ser abertas, sem falhas de decodificação. A busca nas fontes de produção, scripts e launcher não encontrou referências a SVG ou à pasta temporária de inspeção. Nenhum arquivo de imagem ou dependência foi alterado. Permanecem os avisos anteriores das três árvores ausentes; esta verificação não constitui uma nova inspeção visual completa do jogo nem uma revalidação das falhas de vídeo documentadas acima.

Os testes CharacterSpriteRegressionTest, CharacterSelectionRegressionTest e WorldRendererRegressionTest passaram com as dependências atuais. git diff --check também passou.

## Etapa 3 — fundos PNG, abertura, música ambiente e organização (20/11/2026)

A nova solicitação autorizou a troca dos fundos e do menu usando PNGs, o carregamento inicial, as músicas ambiente e a limpeza dos recursos substituídos. A decisão anterior de não integrar SVG continua respeitada. Foram mantidos JDK, distribuições JavaFX, sprites, animações, câmera, colisões, ordem das fases e contratos de save.

### Recursos integrados

| Recurso anterior / origem recebida | Caminho atual | Tela ou fase | Uso em execução |
| --- | --- | --- | --- |
| `Image/Cenarios/FundoDoJogo.jpeg` | `Image/Cenarios/BackGrounds/BackgroundHunter.png` | Curupira / caça ilegal | `Phase.CURUPIRA.background`; cenário da fase 1. |
| Mesmo fundo genérico | `Image/Cenarios/BackGrounds/BackgroundParkour.png` | Saci / parkour | `Phase.SACI.background`; cenário da fase 2. |
| Mesmo fundo genérico | `Image/Cenarios/BackGrounds/BackGroundFire.png` | Caipora / queimadas | `Phase.CAIPORA.background`; cenário da fase 3. |
| Mesmo fundo genérico | `Image/Cenarios/BackGrounds/BackGroundLixo.png` | Negrinho / resíduos | `Phase.NEGRINHO.background`; cenário da fase 4. |
| `Image/Imagens/MENUDOJOGO.png`; substitui `MENU.jpg` e o título sobreposto | `Image/Cenarios/Menu/MENU.png` | Menu e suas telas derivadas | Arte nova com título incorporado. Os cinco comandos ficam à direita, na ordem existente, com mouse e teclado. |
| `Image/Imagens/CarregamentoDOJOGO.png` | `Image/Cenarios/Menu/Carregamento.png` | Abertura anterior ao menu | Imagem exibida enquanto os recursos reais são preparados. |
| `Image/AudioJOGO/MenuTheme.wav` | `Audio/MenuTheme.wav` | Menu, slots, nome, seleção e telas do menu | Loop contínuo de `AmbientAudio.Track.MENU`. |
| `Image/AudioJOGO/ForestTheme.wav` | `Audio/ForestTheme.wav` | Introdução de fase, exploração, desafio, pausa, mapa, diálogo, quiz e encerramento | Loop de `Track.FOREST`; configurações abertas pela pausa preservam essa faixa. |
| `src/main/resources/Image/Cenarios/Menu/Configuracoes/configuracao.png` | `Image/Cenarios/Menu/Configuracoes/configuracao.png` | Configurações | Mesmo arquivo, carregado pelo novo endereço. |
| `src/main/resources/Image/Cenarios/Menu/Pausa/indicador.jpg` | `Image/Cenarios/Menu/Pausa/indicador.jpg` | Pausa | Mesmo arquivo e tratamento de transparência existentes. |
| `src/main/resources/Image/Cenarios/Menu/Pausa/pausa.png` | `Image/Cenarios/Menu/Pausa/pausa.png` | Pausa | Mesmo arquivo. |
| `src/main/resources/Image/Cenarios/Menu/Slots/Como-chama.jpg` | `Image/Cenarios/Menu/Slots/Como-chama.jpg` | Nome | Recorte preparado uma vez; a imagem completa deixa de ficar retida na sessão. |
| `src/main/resources/Image/Cenarios/Menu/Slots/Escolha-Slot.png` | `Image/Cenarios/Menu/Slots/Escolha-Slot.png` | Slots | Mesmo arquivo. |
| `src/main/resources/Image/Cenarios/Menu/Slots/Slot-1.png` | `Image/Cenarios/Menu/Slots/Slot-1.png` | Slot 1 | Mesmo arquivo. |
| `src/main/resources/Image/Cenarios/Menu/Slots/Slot-2.png` | `Image/Cenarios/Menu/Slots/Slot-2.png` | Slot 2 | Mesmo arquivo. |
| `Image/Cenarios/BackGrounds/BackgroundFinal.png` | Preservado | Final futuro | Não associado às quatro fases atuais. |

Os quatro fundos de fase têm 4096 × 800 pixels na origem. `GameAssets` prepara versões de 3288 × 642 pixels, cobrindo a área do mundo abaixo do HUD com proporção preservada dentro do arredondamento de um pixel. `WorldRenderer` desenha essas imagens sem redimensioná-las a cada quadro. O excesso horizontal fica centralizado; os limites lógicos do mundo continuam em 0 e 3200.

O menu recebido tem 1070 × 622 pixels e o carregamento tem 1672 × 941. A interface continua usando 1280 × 720 como resolução lógica e mantém o tratamento de telas com outra proporção. Não há um segundo título desenhado sobre a nova capa. Botões e retratos são ajustados uma vez no carregamento; as dimensões físicas do personagem permanecem em 42 × 58 e o desenho em 58 × 78.

### Carregamento e ciclo de vida

`Main` mostra `LoadingScreen` antes de iniciar um `SwingWorker`. `GameAssets.load` decodifica as imagens, prepara fundos, botões, recortes e sprites e verifica os cabeçalhos dos WAVs. O menu só é anexado depois da conclusão dos recursos e da confirmação assíncrona `CinematicManager.ready()`. Não há temporizador de duração estimada nem espera artificial; falhas de carregamento mostram uma mensagem e oferecem nova tentativa. Sair invalida a conclusão pendente.

A primeira inspeção com janela real encontrou um bloqueio entre a inicialização de `javafx.scene.Node` na thread JavaFX e de `NodeHelper` em `JFXPanel.addNotify` na EDT. O dump está em `out/project-tests/startup-deadlock-before.txt`. A confirmação `ready()` passou a ordenar a criação do painel antes de anexá-lo à janela, sem bloquear a EDT aguardando um Future. Um teste novo abre a janela de produção em processo novo e verifica a transição para o menu; passou nos dois SDKs existentes.

Os arquivos longos de áudio e vídeo são reproduzidos por leitura progressiva. Não se decodificam músicas e vídeos inteiros na memória durante a abertura. O áudio usa Java Sound do próprio JDK: um worker, uma saída de 16 KiB e um buffer de leitura de 4 KiB. Trocar de tela altera a faixa desejada uma vez; atualizações repetidas não reiniciam a música. As cinemáticas solicitam silêncio, preservando o som próprio do vídeo. Fechar ou remover o jogo encerra o worker e libera a saída. Indisponibilidade de um dispositivo de áudio é registrada sem bloquear o jogo.

Os WAVs fornecidos são PCM de 16 bits, estéreo, 44100 Hz: menu com 82,11 s e floresta com 82,29 s. A análise confirmou dados não silenciosos; os arquivos originais foram preservados ao mover. A validação física comprova avanço de frames no mixer, mas não equivale a uma avaliação humana da qualidade percebida nas caixas de som.

### Limpeza e distribuição

- Removidos os arquivos substituídos `FundoDoJogo.jpeg`, `Menu/MENU.jpg` e `Menu/Titulo.png` e a variante não referenciada `Menu/Pausa/indicador.png`. O indicador JPG ativo foi preservado. As quatro remoções somam 1.066.781 bytes nos fontes.
- Removidas as cópias antigas de `FundoDoJogo` em `out/`; a busca por esse nome, incluindo arquivos ignorados no projeto, não encontrou arquivos remanescentes. Referências nos registros históricos descrevem o recurso removido.
- Os sete recursos de interface movidos mantêm os mesmos hashes de objetos Git dos endereços anteriores. Não houve sobrescrita de arquivos de destino.
- Removidos os diretórios vazios `Image/Imagens`, `Image/AudioJOGO`, `resources` e `src/main/resources`. Imagens e áudio agora têm raízes próprias, sem duplicação entre pastas de fontes.
- Removidos os temporários da tentativa SVG em `out/art-integration` e as cópias antigas `out/Image`, `out/Videos`, `out/br`, `out/png-verification`, `out/diagnostic-batch` e os recursos desatualizados copiados em `out/project-tests/classes`. A limpeza foi feita em caminhos locais verificados, sem usar exclusão forçada. Logs, imagens de verificação e dados de diagnóstico pertinentes foram preservados em `out/`.
- `scripts/build-project.ps1` prepara `out/runtime`, copia `Image`, `Audio` e `Videos` e registra os recursos num manifesto. Em builds seguintes, remove apenas recursos anteriormente registrados que deixaram de existir, com verificação de que o caminho permanece dentro da saída. `run.bat` usa esse script. O VS Code compila em `out/vscode`, separado dos testes e da distribuição.
- A conferência final encontrou dois caminhos do VS Code apontando para o diretório inexistente `C:\Program Files\Java\jdk-27.0.1.0.3`. Eles passaram a apontar para o JDK já instalado e usado nas verificações, `C:\Program Files\Java\jdk-27`. Isso corrige a configuração local sem instalar ou atualizar Java.
- Greg, as quatro folhas masculinas, guardiões, artes de vilões, fundo final e vídeo final foram preservados. Recursos reservados para uso futuro não foram descartados por ausência de referência nas quatro fases atuais. A conferência de 140 arquivos versionados em personagens, vídeos originais e `lib` não encontrou divergências.
- As alterações que já existiam em catálogo de vídeos, launcher, renderizador e testes foram preservadas. Esta etapa não modificou `Videos/cinematics.properties`, `GameFlowRegressionTest` nem os arquivos MP4; suas diferenças em relação ao Git vêm da etapa anterior.

### Verificações desta etapa

Ambiente: Windows, JDK e javac 27; compilação com `--release 25`; JavaFX 25.0.1 de `lib/javafx` e 27+30 de `lib/javafx-sdk-27/lib`, sem atualização. Os saves dos testes são temporários; a observação manual por `run.bat` usou `-Duser.home` apontando para `out/manual-validation-home`.

| Verificação / comando | Resultado observado |
| --- | --- |
| `scripts/build-project.ps1` | Passou; compilou os fontes com JavaFX 25.0.1 e preparou os recursos em `out/runtime`. |
| `scripts/test-project.ps1 -Suite Presentation -Release 25` | Passou com JavaFX 27: sprites, seleção, arte preparada, WAVs, fundos/menu/carregamento e janela real. |
| `scripts/test-project.ps1 -Suite Presentation -JavaFxPath lib/javafx -Release 25` | Os cinco testes de apresentação existentes naquele momento passaram; após a correção da inicialização, `StartupRegressionTest` passou separadamente nesse SDK. A regressão final de apresentação foi recompilada e passou com os fontes finais. |
| `AmbientAudioRegressionTest` | Passou; decodificação e repetição integral das duas músicas, escrita parcial, troca, silêncio, chamadas repetidas, no máximo uma saída e liberação do worker. O reforço final do teste confere dois WAVs completos da floresta separadamente da contagem do menu. |
| `AmbientAudioDeviceTest` com JavaFX 25.0.1 | Passou: menu e floresta avançaram mais de 22050 frames cada na saída física; a saída anterior foi fechada e o silêncio liberou a nova. |
| `PresentationRegressionTest` | Passou; correspondência real de pixels dos quatro fundos, proporção, cobertura, reutilização e renderização nas câmeras 0 e 1920; cinco opções à direita sem sobreposição; clique nas resoluções 1280 × 720, 1536 × 864 e 1600 × 1000; progresso, EDT responsiva, conclusão única, cancelamento e recuperação de falha. |
| `StartupRegressionTest` | Passou nos dois SDKs, em processos novos com janela real. As rodadas após a correção registraram 443 pulsos EDT no 25 e 474 no 27 até a abertura do menu. Essas contagens não são um benchmark de FPS. |
| `cmd /c run.bat` | Passou; foram observados carregamento, transição para menu, arte sem título duplicado, opções à direita, Novo jogo → slots, retorno e Configurações. A janela foi fechada e o processo terminou com código 0. |
| Distribuição final fora da raiz | Passou. `out/distribution-check/game.jar`, construído a partir de `out/runtime`, foi usado com diretório de trabalho vazio `out/distribution-check/work`. Apresentação e janela real passaram sem depender dos caminhos de importação ou dos fontes de imagens. A regressão de WAVs empacotados também passou. |
| Auditoria do pacote | Passou: os 50 recursos registrados no manifesto têm SHA-256 idêntico ao arquivo fonte correspondente; nenhuma das quatro imagens removidas está no JAR. |
| `GameFlowRegressionTest` com JavaFX 25.0.1 | Passou: quatro capítulos, términos naturais, suspensão durante vídeo, objetivos, quiz, todas as telas, saves, continuação e reentrada da fase 2. Log: `out/project-tests/gameflow-javafx25.log`. |
| `CinematicResourceRegressionTest` com JavaFX 25.0.1 | Passou: três cancelamentos com players descartados; memória observada 271,6 → 275,3 → 275,9 MiB. |
| `CinematicRegressionTest` com JavaFX 25.0.1 | Falhou no ensaio isolado ao preparar `CenaFase2.mp4`, com `ERROR_MEDIA_INVALID` mesmo após a tentativa de recuperação. A campanha passou em outro processo. A falha transitória conhecida do decoder continua possível; não se declara a suíte inteira verde. Log: `out/project-tests/media-javafx25-corrected-harness.log`. |
| `CinematicResourceRegressionTest` com JavaFX 27+30 | Falhou na regressão conhecida de memória: 593,3 → 935,3 MiB entre os dois primeiros ciclos, crescimento de 342 MiB apesar do descarte. Nenhum SDK foi alterado. Log: `out/project-tests/resources-javafx27.log`. |
| `git diff --check`, paths de recursos e preservação de arquivos | Passou; sem referências de execução a SVG, FundoDoJogo ou ao menu/título antigos. O prefixo legado de `AssetManager.normalize` continua aceito por compatibilidade. |

Um problema adicional do comando de registro foi corrigido em `test-project.ps1`: no PowerShell 5, redirecionar stderr com preferência `Stop` interrompia o harness quando um teste de recuperação emitia um erro intencional, mesmo sem falha do processo Java. O script agora mantém a verificação do código de saída, tolerando stderr. Os resultados de vídeo acima são os códigos reais após essa correção, e não apenas a presença de mensagens no console.

### Conferência visual e limites

Foram inspecionadas as imagens renderizadas do menu, carregamento e os oito enquadramentos de início/fim das quatro fases, em `out/project-tests/artifacts/presentation`. Não foram observadas faixas vazias ou distorção nos fundos nesses enquadramentos. A inspeção nativa em 1536 × 864 confirmou a abertura e os comandos do menu, slots e configurações. A campanha automatizada renderizou as demais telas.

Não foi feita uma nova partida humana completa com avaliação contínua de movimentação e áudio das quatro fases, nem medição comparativa de FPS/p95 desta etapa. Também não houve execução em JDK 25, Linux ou macOS. As árvores opcionais continuam ausentes no conjunto recebido; o carregador apenas evita avisos para decoração deliberadamente opcional. A regressão de 56 poses e os hashes confirmam preservação dos sprites, sem uma nova alegação de funcionamento visual perfeito.

### Melhorias sugeridas

Uma melhoria futura possível é acrescentar controle de volume nas configurações. A tela atual oferece texto, contraste e controles; não há ajuste de música. Os WAVs também têm níveis RMS diferentes (aproximadamente -20,1 dBFS no menu e -31,7 dBFS na floresta). Um ajuste de volume permitiria adequar o ambiente ao equipamento e à preferência do jogador. Prioridade baixa; esforço/risco baixo a médio, com teste de silêncio, retomada e persistência sem alterar saves antigos. Não foi implementado nesta etapa, e os arquivos recebidos não foram normalizados ou regravados.

**Resultado:** fundos, arte de menu, opções à direita, carregamento real, loops de áudio e organização foram implementados e verificados. A validação global de vídeo permanece parcial pelas falhas nativas descritas acima, sob a decisão do usuário de manter as dependências atuais.

## Etapa 4 — guardiões, objetivos, quiz, pulo de cenas e celebração

### Comportamento entregue

- A interrogação foi movida para o canto inferior esquerdo. O painel retangular escuro atrás das opções foi removido; Novo jogo, Continuar, Configurações e Sair continuam à direita, na ordem existente.
- Os guardiões aparecem no cenário. E ou um clique quando o jogador está próximo abre a interação. Ao tentar sair sem concluir os objetivos, o guardião apresenta uma mensagem com as quantidades restantes. Esc ou o comando clicável Voltar à trilha fecha o diálogo ou o quiz sem reabertura automática imediata.
- Após concluir os objetivos e acertar cinco questões, aparece um agradecimento próprio do guardião. O próximo capítulo só abre ao avançar esse agradecimento. Sair da conversa preserva a conclusão; o save retoma o agradecimento pendente. Perder as vidas depois de já ter conquistado essa conclusão não apaga os objetivos creditados.
- As 32 questões foram reescritas com alternativas comparáveis, posições autorais equilibradas e ordem das respostas embaralhada a cada rodada. A resposta correta é remapeada junto com as alternativas. No banco, a maior frase é correta em 11 de 32 questões, incluindo empates; a menor em 14. Portanto, escolher sistematicamente a maior frase deixa de resolver o questionário. O teste também verifica que a resposta pode aparecer nas quatro posições e que o banco original não é modificado.
- Cada cena tem Pular esta cena no canto superior direito, também acionável por P. O pulo está disponível em carregamento, reprodução e falha; avança uma cena por ação e libera o player anterior. Segurar P não percorre várias cenas. Callbacks antigos continuam invalidados. Esc mantém o comportamento de cancelar a sequência e voltar.
- A Celebração da Floresta abre após os agradecimentos do quarto capítulo, com BackgroundFinal.png, piso contínuo, os quatro guardiões e mensagens próprias de agradecimento. Não há inimigos, fogos ou novas coletas. Uma quinta opção na seleção de fases permanece bloqueada até a campanha ser concluída. A exploração, pausa, mapa e save continuam funcionando nessa área.
- A nova plataforma está em Image/Cenarios/Elementos/plataforma.png. A textura inteira é opaca, preparada em 800 × 400, e cada arte de plataforma ocupa a largura e altura exatas do retângulo físico. A superfície coincide com a primeira linha visível, removendo o deslocamento de 9 pixels. Detalhes do arquivo, origem e prompt estão em PLATAFORMAS.md.

### Objetivos reais

| Capítulo | Itens presentes / condição | Regra e interface atuais |
| --- | --- | --- |
| Curupira | 17 sementes e 3 armadilhas | Todas as sementes e armadilhas; HUD e apresentação usam o tamanho das listas. |
| Saci | Travessia até x = 3020 | Marca a travessia ao alcançar a saída; o guardião pode lembrar a tarefa antes disso. |
| Caipora | 9 focos e 2 fontes de água | Apagar os 9 focos. O balde vazio depois do último foco não impede a conclusão. |
| Negrinho | 11 resíduos | Recolher todos os 11; HUD, apresentação e lembrete usam essa quantidade. |
| Celebração | Quatro guardiões | Conversas comemorativas, sem novo requisito de coleta ou quiz. |

Os índices 0–3 dos capítulos e os formatos dos arquivos de save foram preservados. A área final utiliza o campo opcional celebration dentro dos mesmos Properties, junto de campaignComplete. quizComplete e traversalComplete também são opcionais. Saves antigos sem os campos continuam aceitos; campanhas antigas concluídas abrem a celebração. Um save parcial antigo com 12 sementes continua com essas 12 e recebe o lembrete das cinco faltantes, sem perder o progresso.

### Travamentos e medições

O desenho repetia composição de fundo, chão, plataformas, filtros e transformações por quadro. WorldRenderer passou a preparar uma única camada estática opaca para o cenário atual; objetos ativos, jogador e HUD continuam dinâmicos. A camada é invalidada ao trocar capítulo, celebração ou vegetação. Texturas de plataformas são reutilizadas por largura e altura, inclusive em reinícios, e fontes de texto são reutilizadas.

Para a execução normal, VS Code e scripts agora usam o JavaFX 25.0.1 que já existia em lib/javafx, assim como run.bat. Não foi instalada nem atualizada nenhuma dependência; o SDK 27 permanece disponível para diagnóstico. Isso evita escolher por padrão o runtime 27+30 que já reproduziu crescimento de memória nativa durante vídeo. O decoder do 25.0.1 ainda apresenta a falha transitória registrada abaixo; selecionar esse runtime não é apresentado como correção de todo erro de vídeo.

Ambiente dos benchmarks: Windows, JDK 27, JavaFX 25.0.1, -Xms256m -Xmx512m, superfície 1536 × 864. FramePacingBenchmark usa 200 quadros de aquecimento e 600 amostras por capítulo, com o mesmo percurso de câmera. A referência foi executada antes de alterar o carregamento e o renderizador, usando a distribuição anterior. Os arquivos estão em out/updates-gameplay/frames-before.txt e frames-after.txt.

| Capítulo | p95 antes | p95 depois | p99 antes | p99 depois |
| --- | --- | --- | --- | --- |
| Curupira | 7,680 ms | 4,588 ms | 9,090 ms | 5,219 ms |
| Saci | 7,157 ms | 3,351 ms | 8,145 ms | 4,645 ms |
| Caipora | 8,528 ms | 2,763 ms | 11,728 ms | 3,363 ms |
| Negrinho | 7,585 ms | 2,697 ms | 8,710 ms | 3,686 ms |

O probe com janela real, movimentação/pulos e 600 intervalos medidos, após 90 amostras de aquecimento, registrou:

| Métrica na janela | Antes | Depois |
| --- | --- | --- |
| p95 de pintura do RepaintManager | 15,062 ms | 7,516 ms |
| p95 do intervalo do monitor EDT | 18,844 ms | 18,060 ms |
| p99 do intervalo do monitor EDT | 22,790 ms | 19,773 ms |
| Maior intervalo observado | 62,973 ms | 34,917 ms |
| Tempo de GC no trecho medido | 0 ms | 0 ms |

A redução de pintura foi de aproximadamente 50%. As amostras mostram redução de custo e picos, não uma garantia de ausência permanente de qualquer atraso no sistema. Os números de intervalo do monitor EDT não devem ser apresentados como FPS exato. Dados e código reprodutível: FramePacingBenchmark, FramePacingLiveProbe, native-before.txt e native-after.txt em out/updates-gameplay.

### Verificações e resultados

| Verificação | Resultado |
| --- | --- |
| Compilação de todas as fontes e testes, --release 25, com JavaFX 25.0.1 | Passou. |
| scripts/test-project.ps1 -Suite Gameplay -Release 25 | Passou: sprites, seleção, arte preparada, áudio, apresentação, janela de abertura, quiz, atualização de jogabilidade, pulo e campanha completa. Log em out/updates-gameplay/gameplay-suite.log. |
| GameplayUpdateRegressionTest, rodada final | Passou: contagens, lembretes, saída por teclado/mouse, cinco acertos, agradecimentos antes de avançar, retomada de quiz concluído, morte após conclusão, todas as 39 plataformas, final bloqueado por teclado/mouse/chamada interna, seleção final, quatro guardiões, save da posição final e saves antigos completos/parciais. |
| QuizBankRegressionTest | Passou: 32 questões, alternativas distintas, posições equilibradas, ausência do padrão universal de comprimento e preservação da resposta ao embaralhar. |
| CinematicSkipRegressionTest | Passou: primeiro vídeo realmente em PLAYING, pulo por tecla/botão/API, player DISPOSED, tecla repetida, callback antigo, falha e cancelamento. Inclui screenshot do controle. |
| GameplayUpdateRegressionTest com JavaFX 27 existente | Passou, além da compilação alternativa. Não foi feita outra campanha longa de vídeo nesse SDK. |
| Distribuição em JAR com cwd vazio | Passou: GameplayUpdateRegressionTest e QuizBankRegressionTest usando só o JAR e as classes de teste, fora das pastas de recursos do projeto. |
| Alfa do PNG original da nova plataforma | Passou: 1774 × 887, todos os pixels opacos. SHA-256 registrado em PLATAFORMAS.md. |
| Conferência nativa por run.bat, perfil isolado | Passou: menu sem painel, interrogação à esquerda abrindo ajuda, seleção de save concluído, celebração com fundo correto, conversa com Curupira e saída por clique. Perfil em out/updates-gameplay/ui-home, sem usar saves reais. |
| CinematicWindowProbe, modo recovery | Passou na janela: P levou da cena 1 à 2; clique no botão concluiu a segunda, com um único callback. Os arquivos ausentes eram intencionais e impediram término automático durante a inspeção. |
| CinematicResourceRegressionTest com 25.0.1 | Passou: 287,6 → 305,8 → 304,1 MiB, três players descartados. Duas falhas de preparação foram recuperadas pelo mecanismo existente. |
| scripts/test-project.ps1 -Suite Cinematics -Release 25 | Falhou em CinematicRegressionTest ao preparar CenaFase2.mp4, ERROR_MEDIA_INVALID após a tentativa automática. A campanha completa e o teste novo de pulo passaram em outros processos. Esse erro já existia na referência anterior; não se declara toda a suíte verde. Log em out/updates-gameplay/cinematics-suite.log. |
| git diff --check e preservação dos recursos | Passou na revisão final, incluindo a conferência de espaços finais nos arquivos novos. Bibliotecas, MP4s originais e sprites permaneceram intactos. |

As falhas inicialmente encontradas no novo teste de plataformas e no sorteio foram investigadas antes de repetir: a primeira era dano causado por fogo na própria plataforma durante a medição de geometria, isolado na fixture; a segunda usava o primeiro valor de Random com sementes consecutivas 0–99, que não exercitava todas as permutações. O teste passou a usar uma sequência pseudoaleatória determinística contínua, mantendo a exigência das quatro posições. Nenhuma dessas correções relaxou as regras do jogo.

### Arquivos desta etapa

Produção: Game.java, WorldRenderer.java, GameAssets.java, GameConfig.java, CinematicManager.java, Question.java, QuestionBank.java e plataforma.png. Configuração/documentação: .vscode/launch.json, .vscode/settings.json, scripts/test-project.ps1, scripts/test-cinematics.ps1, README.md, VIDEOS.md, PLATAFORMAS.md e este registro. Testes novos: GameplayUpdateRegressionTest, QuizBankRegressionTest, CinematicSkipRegressionTest, FramePacingBenchmark, FramePacingLiveProbe e CinematicWindowProbe; GameFlowRegressionTest e PresentationRegressionTest foram ajustados ao novo comportamento.

O diff inicial foi preservado em out/updates-gameplay/initial-worktree.patch. Alterações anteriores do launcher, catálogo, carregamento e áudio foram mantidas. Não houve commit, push, mudança global, alteração dos MP4s, de bibliotecas ou de sprites. A imagem anterior de plataforma foi preservada apenas como comparação em out/, e a nova imagem está no caminho consumido pelo jogo e incluído na distribuição.

### Limites da validação

Os novos fluxos e a campanha foram verificados, incluindo a área final. A inspeção visual cobre as imagens geradas pelos testes, o menu e a celebração na janela e os controles do reprodutor. Não foi feita uma nova partida humana contínua de todas as fases, nem validação em outros computadores ou no JDK 25. O erro transitório de vídeo do 25.0.1 continua possível; a recuperação e o pulo explícito permanecem disponíveis. O SDK 27 continua com a limitação histórica de memória e deixou de ser o padrão de execução.

A decisão de manter as imagens raster foi reconfirmada em 20/11/2026: não há referências a SVG ou `IMAGENS_SVG` no código de execução, nos scripts ou na configuração do VS Code. A pasta `Image` contém 40 PNGs e dois JPGs legados. Nenhum conversor ou biblioteca SVG foi adicionado, e as versões das dependências foram preservadas.

Na conferência final, todas as fontes principais e testes compilaram novamente com `--release 25`. `GameplayUpdateRegressionTest`, `WorldRendererRegressionTest` e `PresentationRegressionTest` passaram, incluindo a verificação de todos os pixels da plataforma preparada. O build atual contém 50 recursos e nenhum SVG; o PNG da plataforma copiado corresponde ao hash do original. O teste de jogabilidade também passou usando apenas `out/runtime` e classes de teste no classpath, executado fora da pasta do projeto.

A tentativa adicional de renovar o JAR parou com `IOException: Espaço insuficiente no disco`, antes dos testes dessa nova embalagem. O JAR de diagnóstico anterior foi removido para liberar espaço; não deve ser usado como distribuição atual. A aprovação do JAR na tabela acima pertence à rodada anterior, enquanto a compilação e os testes finais verificaram a distribuição em pasta. Nenhum arquivo-fonte, recurso original ou dependência foi removido nessa limpeza.
