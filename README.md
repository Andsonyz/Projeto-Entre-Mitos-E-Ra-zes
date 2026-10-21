# Entre Mitos e Raízes

Jogo educativo de plataforma 2D inspirado no folclore brasileiro. Explore a floresta, cumpra desafios ambientais e aprenda com Curupira, Saci, Caipora e Negrinho do Pastoreio.

Desenvolvido em **Java**, com **Swing/AWT** para a interface e **JavaFX Media** para os vídeos.

## Funcionalidades

- Quatro fases com objetivos, diálogos e desafios educativos.
- Dois slots de save, continuação da campanha e seleção de fases desbloqueadas.
- Prólogo em vídeo, mapa de progresso e menu de pausa.
- Seleção de personagem: masculino disponível e feminino identificado como “Em breve”.
- Ajustes de tamanho do texto e alto contraste.

## Requisitos e compatibilidade

O projeto está configurado para **Windows** e possui duas opções de execução:

| Opção | JDK | JavaFX | Local das bibliotecas |
| --- | --- | --- | --- |
| Script `run.bat` | 25 ou superior | 25.0.1 | `lib/javafx/` |
| VS Code | 27 | 27 | `lib/javafx-sdk-27/lib/` |

A compilação e a reprodução das duas cenas foram verificadas com **JDK 27 nas duas versões do JavaFX**. Com JavaFX 25.0.1, também foi verificada a compilação com `--release 25`. A execução no JDK 25 não foi testada.

Mantenha as pastas `Image/`, `Videos/` e `src/main/resources/` nos locais originais. O projeto compila diretamente com `javac`, sem Maven ou Gradle.

## Como executar

### Pelo VS Code

1. Abra a pasta raiz do projeto no VS Code, com o suporte a Java instalado.
2. Confira o caminho do JDK 27 em `.vscode/settings.json`.
3. Verifique se o SDK JavaFX 27 está em `lib/javafx-sdk-27/`.
4. Em **Executar e Depurar**, selecione **Entre Mitos e Raizes - JavaFX 27** e pressione **F5**.

O arquivo `.vscode/launch.json` define a classe principal, os módulos JavaFX e o diretório de execução.

### Pelo terminal

Com `java` e `javac` da mesma versão no `PATH` e o JavaFX 25.0.1 em `lib/javafx/`, execute na raiz do projeto:

```powershell
.\run.bat
```

O script compila os fontes, copia os recursos para `out/` e inicia o jogo. Ele utiliza **JavaFX 25.0.1**, mesmo quando o JDK instalado é o 27.

## Controles

| Tecla ou ação | Função |
| --- | --- |
| `A` / `D` ou setas laterais | Mover o personagem |
| `Shift` + movimento | Andar; solte `Shift` para correr |
| `W`, seta para cima ou `Espaço` | Pular |
| `Enter` | Confirmar e avançar diálogos |
| `E` | Interagir com armadilhas, fontes e focos de incêndio; avançar diálogos |
| `M` | Abrir ou fechar o mapa |
| `F5` | Salvar durante a exploração |
| `Esc` | Pausar ou voltar |
| `1` a `4` | Selecionar uma resposta no quiz; confirmar com `Enter` ou `Espaço` |
| `Delete` / `R` | Excluir / reiniciar a campanha do slot selecionado, após confirmação |
| Mouse | Navegar pelas telas e interagir com elementos próximos |

## Campanha

| Fase | Guardião | Objetivo |
| --- | --- | --- |
| Os Rastros da Caça | Curupira | Coletar 12 sementes e desativar 3 armadilhas. |
| A Travessia do Saci | Saci | Atravessar plataformas e obstáculos. |
| Entre Cinzas e Raízes | Caipora | Encher o balde nas fontes e apagar todos os focos. |
| Os Rastros do Lixo | Negrinho do Pastoreio | Recolher 10 resíduos. |

Cada fase apresenta a lenda do guardião e termina com um diálogo e um quiz. São sorteadas cinco perguntas por rodada, de um banco de oito por fase. Uma resposta incorreta reinicia a rodada.

### Vídeos antes dos capítulos

Novo jogo segue o fluxo **slot → nome → seleção de personagem → prólogo completo → vídeo do capítulo, se configurado → apresentação da fase → exploração**. O masculino começa selecionado; o feminino permanece bloqueado. `Esc` na seleção retorna ao nome.

Os vídeos `Videos/cena1.mp4` e `Videos/cena2.mp4` compõem o prólogo. Para adicionar cenas ao prólogo ou antes de cada capítulo, edite **[`Videos/cinematics.properties`](Videos/cinematics.properties)**:

```properties
chapter.2.1.video=Videos/capitulo2/entrada.mp4
chapter.2.1.caption=O caminho do Saci começa aqui.
```

As cenas avançam somente ao terminar naturalmente. Durante a reprodução, a fase permanece suspensa. `Esc` cancela e volta à tela anterior, sem liberar a fase. Uma falha oferece **Tentar novamente** e **Voltar**. Veja o [guia de vídeos](docs/VIDEOS.md) para configuração e recuperação.

### Progresso salvo

Os dois slots são armazenados em `%USERPROFILE%\.entremitoseraizes\saves`, nos arquivos `slot-1.properties` e `slot-2.properties`. Eles registram a fase, as fases desbloqueadas, a posição, as vidas e os objetivos concluídos.

A escolha do personagem e os vídeos pendentes usam esses mesmos arquivos. Continuar retoma uma sequência não concluída; uma sequência concluída não se repete. Saves antigos sem esses campos continuam compatíveis e adotam o masculino.

## Organização do código

Os fontes ficam em `src/main/java/br/com/raizes/`:

| Componente | Responsabilidade |
| --- | --- |
| `Main.java` | Inicialização da janela e do jogo. |
| `core/` | Loop, física e campanha em `Game`; seleção de telas em `drawScreen()`; desenho do mundo em `WorldRenderer`. |
| `audio/` | Sequências em `CinematicCatalog`, cenas em `CinematicScene` e reprodução em `CinematicManager`. |
| `character/` | Catálogo de personagens, recorte das folhas e animações por tempo, com âncora nos pés. |
| `assets/AssetManager.java` | Carregamento de imagens e localização de arquivos. |
| `config/GameConfig.java` | Dimensões, constantes e filtros visuais. |
| `model/` | Definições das fases, perguntas e objetos do cenário. |
| `persistence/SaveManager.java` | Leitura, gravação e exclusão dos saves. |
| `quiz/QuestionBank.java` | Banco de perguntas de cada fase. |

Os recursos visuais ficam em `Image/` e `src/main/resources/`; os vídeos, em `Videos/`. A pasta `out/` contém arquivos gerados pela compilação e é ignorada pelo Git.

Para adicionar fases, revise as regras em `Game`, os desenhos em `WorldRenderer`, as definições em `Phase` e as perguntas em `QuestionBank`. Os saves usam índices de fases e de objetos; alterações nessa ordem exigem atenção à compatibilidade com campanhas existentes.

## Verificação

Para verificar vídeos, campanha, controles e saves, execute a suíte com JavaFX 27 ou com JavaFX 25.0.1:

```powershell
.\scripts\test-project.ps1
.\scripts\test-project.ps1 -JavaFxPath 'lib/javafx' -Release 25
```

Os testes usam saves temporários e geram imagens em `out/project-tests/artifacts/`. Cobrem término natural dos vídeos, cancelamento, recuperação, suspensão da fase, seleção, sprites e saves antigos. Consulte os [resultados e limitações da atualização](docs/VALIDACAO-VIDEO-PERSONAGENS.md), os [detalhes dos personagens e recursos](docs/PERSONAGENS.md) e o [registro anterior de revisão](docs/REVISAO.md).
