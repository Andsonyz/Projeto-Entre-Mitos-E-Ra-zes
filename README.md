# Entre Mitos e Raízes

Jogo educativo de plataforma 2D inspirado no folclore brasileiro. Explore a floresta, cumpra desafios ambientais e aprenda com Curupira, Saci, Caipora e Negrinho do Pastoreio.

Desenvolvido em **Java**, com **Swing/AWT** para a interface e **JavaFX Media** para os vídeos.

## Funcionalidades

- Quatro capítulos com objetivos e desafios educativos, seguidos por uma fase comemorativa com os quatro guardiões.
- Dois slots de save, continuação da campanha e seleção de fases desbloqueadas.
- Prólogo em vídeo, mapa de progresso e menu de pausa.
- Seleção de personagem: masculino disponível e feminino identificado como “Em breve”.
- Ajustes de tamanho do texto e alto contraste.
- Carregamento inicial em segundo plano, com arte própria antes do menu.
- Fundos específicos para caça, parkour, queimadas e resíduos.
- Música ambiente em looping: menu e floresta, com silêncio durante as cinemáticas.
- Encerramento pela celebração, vídeo final e créditos dos criadores, com retorno ao menu.
- Simulação da campanha sem deslocamento ou coleta manual, isolada dos saves.
- Modo leve para reduzir o trabalho de renderização em computadores com poucos recursos.

## Requisitos e compatibilidade

O projeto está configurado para **Windows** e possui duas opções de execução:

| Opção | JDK | JavaFX | Local das bibliotecas |
| --- | --- | --- | --- |
| Script `run.bat` | 25 ou superior | 25.0.1 | `lib/javafx/` |
| VS Code | 27 | 25.0.1 | `lib/javafx/` |

A execução padrão usa **JDK 27 e JavaFX 25.0.1**, com compilação `--release 25`. O JDK 25 não foi executado neste ambiente. O SDK JavaFX 27 existente permanece disponível para diagnóstico, com uma limitação conhecida de memória durante vídeos. Nenhuma dependência foi atualizada. Consulte [desempenho e validação](docs/DESEMPENHO.md).

Mantenha as pastas `Image/`, `Audio/` e `Videos/` junto ao projeto. O projeto compila diretamente com `javac`, sem Maven ou Gradle. Os recursos de interface antes separados em `src/main/resources/` foram reunidos em `Image/`.

## Como executar

### Pelo VS Code

1. Abra a pasta raiz do projeto no VS Code, com o suporte a Java instalado.
2. Confira o caminho do JDK 27 em `.vscode/settings.json`.
3. Verifique se o JavaFX 25.0.1 está em `lib/javafx/`.
4. Em **Executar e Depurar**, selecione **Entre Mitos e Raizes - JavaFX 25.0.1** e pressione **F5**.

O arquivo `.vscode/launch.json` define a classe principal, os módulos JavaFX e o diretório de execução.

### Pelo terminal

Com `java` e `javac` da mesma versão no `PATH` e o JavaFX 25.0.1 em `lib/javafx/`, execute na raiz do projeto:

```powershell
.\run.bat
```

Para o modo leve, com heap de até 128 MiB e renderização de até 30 quadros/s:

```powershell
.\run.bat --leve
```

No VS Code, a configuração **Entre Mitos e Raizes - Modo leve** oferece o mesmo perfil. O modo padrão limita o heap a 256 MiB. Esses valores se referem à memória Java; o sistema, a janela e o decoder de vídeo utilizam memória adicional. A física e as regras são iguais nos dois perfis.

O script usa `scripts/build-project.ps1` para compilar e copiar os recursos para `out/runtime/`, depois inicia o jogo. Ele utiliza **JavaFX 25.0.1**, mesmo quando o JDK instalado é o 27. O VS Code utiliza `out/vscode/` para suas classes, separado da distribuição e dos testes.

Para apenas compilar e preparar a distribuição:

```powershell
.\scripts\build-project.ps1
```

A abertura exibe `Image/Cenarios/Menu/Carregamento.png` durante a preparação das imagens e animações. O menu só aparece após a conclusão dos recursos e do painel JavaFX, sem tempo de espera artificial. Os áudios e vídeos longos são lidos durante a reprodução para limitar o uso de memória.

## Controles

| Tecla ou ação | Função |
| --- | --- |
| `A` / `D` ou setas laterais | Mover o personagem |
| `Shift` + movimento | Andar; solte `Shift` para correr |
| `W`, seta para cima ou `Espaço` | Pular |
| `Enter` | Confirmar e avançar diálogos |
| `E` | Conversar com guardiões e interagir com armadilhas, fontes e focos; avançar diálogos |
| `P` ou botão **Pular esta cena** | Pular somente a cena atual do vídeo |
| `M` | Abrir ou fechar o mapa |
| `F5` | Salvar durante a exploração |
| `Esc` | Pausar ou voltar; sair de conversas e questionários para retomar a trilha |
| `1` a `4` | Selecionar uma resposta no quiz; confirmar com `Enter` ou `Espaço` |
| `Delete` / `R` | Excluir / reiniciar a campanha do slot selecionado, após confirmação |
| Mouse | Navegar pelas telas e interagir com elementos próximos |
| `Enter` na celebração | Encerrar a jornada e assistir ao vídeo final antes dos créditos |
| `F8` na simulação | Avançar a etapa atual, incluindo vídeos, objetivos e quiz |

## Campanha

| Fase | Guardião | Objetivo |
| --- | --- | --- |
| Os Rastros da Caça | Curupira | Coletar as 17 sementes e desativar as 3 armadilhas. |
| A Travessia do Saci | Saci | Atravessar plataformas e obstáculos. |
| Entre Cinzas e Raízes | Caipora | Encher o balde nas fontes e apagar os 9 focos. |
| Os Rastros do Lixo | Negrinho do Pastoreio | Recolher os 11 resíduos. |
| Celebração da Floresta | Os quatro guardiões | Explorar o mapa final e conversar com cada guardião; sem perigos ou novas coletas. |

Cada capítulo apresenta a lenda do guardião e termina com um diálogo e um quiz. São sorteadas cinco perguntas por rodada, de um banco de oito por fase; a ordem das alternativas também muda. Uma resposta incorreta reinicia a rodada. `Esc` ou **Voltar à trilha** permite deixar a conversa ou o quiz. Ao retornar ao desafio, a rodada recomeça.

Ao chegar à saída com tarefas pendentes, o guardião informa as quantidades que faltam. Concluir os objetivos e acertar as cinco questões abre um agradecimento antes do próximo capítulo. Após o quarto, a celebração usa `BackgroundFinal.png` e permite conversar novamente com os guardiões por `E` ou clicando neles quando estiver próximo. Campanhas antigas já concluídas também têm acesso a essa área.

Na celebração, **Encerrar / Enter** inicia `CenaFinal.mp4`; ao terminar, aparecem os créditos de **Antonio Andson de Oliveira Rocha** e **Sophia Hellen Pires da Silveira**. O botão **Voltar ao menu**, `Enter` ou `Esc` retorna ao menu. O save concluído continua disponível para explorar a celebração novamente. Os créditos também podem ser consultados em **? → Créditos**.

### Simular sem jogar manualmente

Abra **? → Simular campanha** ou pressione `T` na ajuda. Clique no rodapé **SIMULAÇÃO** ou pressione `F8` para avançar: o controle libera a apresentação, conclui os objetivos, avança as falas e responde às cinco questões pelo mesmo fluxo da campanha. Na celebração, percorre as conversas dos quatro guardiões e o encerramento. Nos vídeos, `F8` pula somente a cena atual; os controles normais continuam disponíveis.

A faixa **SIMULAÇÃO** identifica esse modo. Ele não grava, apaga ou altera nenhum slot. Ao voltar ao menu, o atalho é desativado. Novo jogo e Continuar seguem as regras normais.

### Vídeos antes dos capítulos

Novo jogo segue o fluxo **slot → nome → seleção de personagem → vídeos configurados (com pulo opcional) → apresentação da fase → exploração**. O masculino começa selecionado; o feminino permanece bloqueado. `Esc` na seleção retorna ao nome.

O catálogo atual utiliza `Videos/Cenas/CenaFase1.mp4` a `CenaFase4.mp4`, antes dos capítulos correspondentes. Para adicionar cenas ao prólogo ou antes de cada capítulo, edite **[`Videos/cinematics.properties`](Videos/cinematics.properties)**:

```properties
chapter.2.1.video=Videos/capitulo2/entrada.mp4
chapter.2.1.caption=O caminho do Saci começa aqui.
```

Cada cena avança ao terminar ou quando o jogador escolhe **Pular esta cena** (`P`). Durante a reprodução, a fase permanece suspensa. `Esc` cancela e volta à tela anterior, mantendo a sequência pendente. Uma falha oferece **Tentar novamente**, **Pular esta cena** e **Voltar**. Veja o [guia de vídeos](docs/VIDEOS.md) para configuração e recuperação.

### Progresso salvo

Os dois slots são armazenados em `%USERPROFILE%\.entremitoseraizes\saves`, nos arquivos `slot-1.properties` e `slot-2.properties`. Eles registram a fase, as fases desbloqueadas, a posição, as vidas e os objetivos concluídos.

A escolha do personagem e os vídeos pendentes usam esses mesmos arquivos. Continuar retoma uma sequência não concluída; uma sequência concluída não se repete. Saves antigos sem esses campos continuam compatíveis e adotam o masculino.

Os campos opcionais `quizComplete`, `traversalComplete` e `celebration` preservam o agradecimento pendente, a travessia do Saci e a posição na celebração. Os índices dos quatro capítulos permanecem iguais; saves antigos já concluídos abrem a área comemorativa.

## Organização do código

Os fontes ficam em `src/main/java/br/com/raizes/`:

| Componente | Responsabilidade |
| --- | --- |
| `Main.java` | Inicialização da janela e do jogo. |
| `core/GameAssets.java` e `LoadingScreen.java` | Preparação dos recursos em segundo plano, progresso e recuperação de falha na abertura. |
| `core/` | Loop, física e campanha em `Game`; seleção de telas em `drawScreen()`; desenho do mundo em `WorldRenderer`. |
| `audio/` | Vídeos em `CinematicManager` e sequências em `CinematicCatalog`; música ambiente em `AmbientAudio`. |
| `character/` | Catálogo de personagens, recorte das folhas e animações por tempo, com âncora nos pés. |
| `assets/AssetManager.java` | Carregamento de imagens e localização de arquivos. |
| `config/GameConfig.java` | Dimensões, constantes e filtros visuais. |
| `model/` | Definições das fases, perguntas e objetos do cenário. |
| `persistence/SaveManager.java` | Leitura, gravação e exclusão dos saves. |
| `quiz/QuestionBank.java` | Banco de perguntas de cada fase. |

Os recursos visuais ficam em `Image/`, os WAVs em `Audio/` e os vídeos em `Videos/`. `Image/Cenarios/Menu/` concentra menu, carregamento, slots, pausa e configurações. A pasta `out/` contém arquivos gerados e é ignorada pelo Git.

`Phase.background` associa cada capítulo a uma imagem de `Image/Cenarios/BackGrounds/`. `BackgroundFinal.png` é usado exclusivamente na celebração. Greg e as artes de vilões permanecem preservados para uso futuro. Para trocar músicas, mantenha os nomes `Audio/MenuTheme.wav` e `Audio/ForestTheme.wav`, em WAV PCM compatível com Java Sound. A reprodução usa um canal e um buffer de 4 KiB, sem carregar cada música inteira na memória.

Para adicionar fases, revise as regras em `Game`, os desenhos em `WorldRenderer`, as definições em `Phase` e as perguntas em `QuestionBank`. Os saves usam índices de fases e de objetos; alterações nessa ordem exigem atenção à compatibilidade com campanhas existentes.

## Verificação

Para verificar vídeos, campanha, controles e saves, execute a suíte com JavaFX 27 ou com JavaFX 25.0.1:

```powershell
.\scripts\test-project.ps1
.\scripts\test-project.ps1 -JavaFxPath 'lib/javafx' -Release 25
```

Para verificar esta atualização de apresentação, fundos, áudio e abertura:

```powershell
.\scripts\test-project.ps1 -Suite Presentation -Release 25
.\scripts\test-project.ps1 -Suite Gameplay -Release 25
```

A suíte `Gameplay` acrescenta objetivos, guardiões, agradecimentos, plataformas, quiz, pulo de cenas, celebração e a campanha completa. O runtime padrão agora é o JavaFX 25.0.1 já existente. O SDK 27 permanece em `lib/javafx-sdk-27` para diagnóstico; pode ser selecionado explicitamente com `-JavaFxPath lib/javafx-sdk-27/lib`, mantendo a limitação nativa registrada.

Essa suíte também abre e fecha uma janela real para conferir a transição de carregamento para menu. O teste opcional da saída de áudio física, após a compilação dos testes, é:

```powershell
java --module-path lib/javafx --add-modules javafx.controls,javafx.media,javafx.swing -cp out/project-tests/classes br.com.raizes.audio.AmbientAudioDeviceTest
```

As verificações de poucos recursos podem ser executadas com:

```powershell
.\scripts\test-project.ps1 -Suite Performance -Release 25
```

Esse comando usa heap de 128 MiB e dois processadores disponíveis para a JVM. Testa a preparação repetida dos cinco vídeos, a interface ociosa, a física, a simulação e a campanha completa até os créditos. Veja [desempenho](docs/DESEMPENHO.md) para resultados e limites.

Para gerar e verificar um JAR fora das pastas de recursos:

```powershell
.\scripts\test-package.ps1
```

Os testes usam saves temporários e geram imagens em `out/project-tests/artifacts/`. Cobrem término natural dos vídeos, cancelamento, recuperação, suspensão da fase, seleção, sprites e saves antigos. O [índice da documentação](docs/README.md) separa guias atuais dos registros históricos.
