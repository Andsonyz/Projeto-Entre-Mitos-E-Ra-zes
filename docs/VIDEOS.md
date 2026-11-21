# Vídeos da campanha

As sequências são configuradas em [`Videos/cinematics.properties`](../Videos/cinematics.properties). O arquivo usa UTF-8 e é lido ao iniciar o jogo. Não é necessário alterar ou recompilar o Java para mudar a lista de vídeos; reinicie o jogo após editar a configuração.

## Adicionar uma cena

1. Coloque o arquivo MP4 em `Videos/` ou em uma subpasta.
2. Adicione o caminho em `cinematics.properties`, relativo à raiz do projeto.
3. Acrescente uma legenda se desejar.

Para adicionar uma cena ao prólogo:

```properties
prologue.3.video=Videos/cena3.mp4
prologue.3.caption=Texto apresentado durante a terceira cena.
```

Para reproduzir duas cenas antes do capítulo do Saci:

```properties
chapter.2.1.video=Videos/capitulo2/entrada.mp4
chapter.2.1.caption=O caminho do Saci começa aqui.
chapter.2.2.video=Videos/capitulo2/travessia.mp4
```

| Prefixo | Quando é reproduzido |
| --- | --- |
| `prologue` | Ao criar uma nova campanha, antes do primeiro capítulo. |
| `chapter.1` | Antes da introdução do Curupira. |
| `chapter.2` | Antes da introdução do Saci. |
| `chapter.3` | Antes da introdução da Caipora. |
| `chapter.4` | Antes da introdução do Negrinho do Pastoreio. |

O número após o prefixo define a ordem da cena. A ordenação é numérica: `2` vem antes de `10`, e os números não precisam ser consecutivos. A propriedade `.caption` é opcional; cenas sem legenda exibem o vídeo sem o painel de texto.

## Reprodução e continuidade

- Um capítulo sem vídeos configurados abre diretamente sua introdução.
- O novo jogo abre a seleção de personagem antes do prólogo. Os vídeos do capítulo aparecem ao chegar à fase e ao reiniciá-la pela seleção de fases.
- Uma cena avança por término natural (`MediaPlayer.onEndOfMedia`) ou por escolha explícita de **Pular esta cena**, no canto superior direito, também acessível por `P`. O pulo afeta apenas a cena atual e está disponível durante carregamento, reprodução e recuperação de falha. `Enter`, `Espaço` e cliques fora desse botão não pulam o vídeo. Segurar `P` não pula várias cenas.
- Física, controles, inimigos, animações e relógio da fase ficam suspensos durante carregamento e reprodução. A localização do arquivo acontece fora das threads da interface; um indicador mostra o carregamento.
- O último frame permanece no player até o callback preparar a próxima tela. O player anterior é liberado na troca de cena ou ao sair. Identificadores de sessão/cena descartam callbacks repetidos ou antigos.
- `Esc` cancela sem liberar a fase: retorna à seleção no prólogo de um novo jogo, ou ao menu nas sequências retomadas e nos capítulos.
- Arquivos ausentes, incompatíveis ou com falha exibem **Tentar novamente** e **Voltar**. O erro detalhado aparece no terminal. Mouse e teclado podem usar os botões; `Esc` volta e `Enter` tenta novamente quando nenhum botão tem foco.
- O limite de 12 segundos detecta falha de preparação ou travamento; nunca conclui a cena. Falhas nativas `UNKNOWN` recebem uma tentativa automática após 200 ms; se persistirem, a recuperação fica a cargo do usuário.
- Os campos `pendingPrologue` e `pendingChapter` no save permanecem ativos até todas as cenas terminarem ou serem puladas explicitamente. Cancelar, falhar ou fechar mantém a sequência pendente. **Continuar** recomeça a sequência pendente desde sua primeira cena. Após concluída, ela não se repete ao continuar.
- Saves antigos sem esses campos conservam o comportamento anterior de retomada, abrindo a apresentação da fase. Não há novo formato ou arquivo de save.

## Formato dos arquivos

Use MP4 com vídeo **H.264/AVC**. Para arquivos com som, o formato de áudio previsto pelo JavaFX é **AAC**, sujeito ao suporte do ambiente. Consulte os [formatos aceitos pelo JavaFX](https://openjfx.io/javadoc/27/javafx.media/javafx/scene/media/package-summary.html).

Os arquivos `cena1.mp4` e `cena2.mp4` continham faixas AAC silenciosas que provocavam `ERROR_MEDIA_INVALID` no ambiente analisado. Essas faixas foram removidas, preservando o fluxo de vídeo original, sem recodificação ou perda de qualidade da imagem. A reprodução completa das duas cenas foi verificada com JavaFX 25.0.1 e 27.

Nas verificações atuais houve falhas nativas `UNKNOWN / ERROR_MEDIA_INVALID` intermitentes, inclusive com arquivos válidos que reproduziram integralmente em outras execuções. O tratamento não considera esse erro como fim do vídeo: tenta reinicializar o decoder uma vez e, se necessário, apresenta a recuperação. A causa nativa não foi determinada; a disponibilidade do decoder no computador de destino ainda precisa de observação em janela.

## Verificar a reprodução

Os testes usam o decoder real do JavaFX e não abrem uma janela nem alteram saves. Com JDK 27, execute na raiz do projeto:

```powershell
.\scripts\test-cinematics.ps1
```

Para verificar com JavaFX 25.0.1:

```powershell
.\scripts\test-cinematics.ps1 -JavaFxPath 'lib/javafx'
```

Os testes de cinema cobrem sequências, ordem, legendas, reprodução natural, bloqueio de entradas sem ação de pulo, conclusão única, cancelamento, callbacks antigos e recuperação. `CinematicSkipRegressionTest`, incluído em `test-project.ps1 -Suite Gameplay`, verifica o pulo por tecla, botão e API, repetição da tecla, descarte de player e pulo após falha. A campanha completa também verifica suspensão da fase e retomada pelo save. Avisos de arquivos ausentes/corrompidos são esperados nos testes de recuperação.
