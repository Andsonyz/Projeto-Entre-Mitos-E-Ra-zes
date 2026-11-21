# Desempenho e validação

## Execução

| Perfil | Comando | Heap Java | Renderização |
| --- | --- | --- | --- |
| Padrão | `.\run.bat` | Até 256 MiB | Até aproximadamente 60 quadros/s. |
| Leve | `.\run.bat --leve` | Até 128 MiB | Até aproximadamente 30 quadros/s. |

O perfil leve também é selecionado automaticamente quando a JVM informa até dois processadores ou heap de até 192 MiB. A física continua no temporizador de 16 ms e as animações do jogador usam tempo decorrido. Nenhuma fase, regra, música, vídeo, configuração, animação ou opção de save foi removida.

Heap não representa o consumo total do processo: JavaFX, decoder, áudio, drivers e janela usam memória adicional. A validação em uma máquina Windows não estabelece requisitos mínimos universais. O jogo continua exigindo Windows e JDK/JavaFX compatíveis.

## Alterações

- Imagens pequenas são decodificadas perto do tamanho de uso. O indicador de pausa deixou de manter uma imagem de 2048×2048 e passou a ocupar uma caixa de até 32×44. Os PNGs de origem e os sprites foram preservados.
- Menus e diálogos estáticos repintam quando há entrada, mudança de estado ou mensagem. A cena JavaFX não provoca repintura contínua do jogo suspenso.
- Movimento do mouse e repetição das teclas de movimento não ultrapassam o limite de pintura. A animação de seleção continua quando a opção muda; o cursor continua respondendo imediatamente.
- A composição estática do cenário continua reutilizada. As colisões usam uma caixa física reutilizável e comparações numéricas, sem criar retângulos para cada item em cada atualização.
- Vídeos e WAVs continuam lidos sob demanda. Recursos extraídos de um JAR são reutilizados durante o processo, evitando copiar o mesmo MP4 para outro temporário a cada reprodução.
- O build deixa de copiar recursos cujo tamanho e data correspondem à origem. Arquivos removidos da origem continuam sendo retirados da distribuição pelo manifesto.
- A preparação do arquivo e a preparação do decoder têm limites separados, com recuperação visível. Nenhum desses limites avança a campanha; o término natural ou o pulo explícito continua controlando a transição.

## Vídeos

Os originais anteriores à otimização foram copiados e conferidos por SHA-256 em `out/backups/videos/`. Os diagnósticos ficam em `out/performance/`; não são recursos de execução.

A preparação usa FFmpeg apenas como ferramenta de desenvolvimento, fora das dependências do jogo. O executável foi extraído do pacote Windows `imageio-ffmpeg 0.8.1`, após conferir o SHA-256 publicado pelo PyPI. A origem e o hash estão em `out/tools/ffmpeg-source.json`. O jogo distribuído não usa FFmpeg nem exige Python.

Para preparar novas cópias, com um executável FFmpeg disponível:

```powershell
.\scripts\prepare-videos.ps1 -FfmpegPath 'caminho\ffmpeg.exe'
```

O comando escreve em `out/video-candidates/` e não substitui os originais. O perfil é H.264 Constrained Baseline 3.1, 960×540, 30 fps, yuv420p, AAC estéreo a 44,1 kHz/128 kbit/s e índice MP4 no início. Trata-se de recodificação com perda para reduzir custo e tamanho; a trilha sonora e o conteúdo das cenas permanecem presentes. Antes de adotar novas cópias, verifique início, término natural, áudio e reprodução em janela.

## Testes

```powershell
.\scripts\test-project.ps1 -Release 25
.\scripts\test-project.ps1 -Suite Performance -Release 25
```

A suíte completa cobre sprites, seleção, apresentação, abertura, áudio, objetivos, guardiões, quiz, pulo, campanha, créditos e saves antigos. `Performance` executa os testes de interface ociosa, decoder, simulação e campanha completa com `-Xmx128m -XX:ActiveProcessorCount=2`.

`EndingSimulationRegressionTest` percorre os capítulos, os quatro agradecimentos finais e os créditos pelos controles de simulação, verificando os dois saves byte por byte. `GameFlowRegressionTest` percorre a campanha normal e aguarda os términos naturais, incluindo `CenaFinal.mp4`. A simulação é acessível pela ajuda e não grava progresso.

### Diagnóstico realizado

O código anterior falhou com `OutOfMemoryError` durante o carregamento com heap de 128 MiB. Após reduzir as imagens de interface, o carregamento e a renderização dos quatro capítulos passaram no mesmo limite. O teste de interface ociosa registrou zero repinturas em 600 ms; no modo leve, 18 solicitações de pintura e cerca de 157 pixels de deslocamento no mesmo intervalo. Isso comprova que limitar a pintura não reduziu a velocidade da física nesse ensaio.

As primeiras cópias de vídeo preservavam o AAC de origem. Elas passaram em uma rodada curta, mas falharam na 11ª inicialização de uma rodada maior. O log nativo registrou `dshowwrapper / Failed to decode stream`; não houve falha de caminho nem término natural. Um controle sem áudio passou por 25 preparações. Por isso o áudio foi incluído na padronização, em vez de ser removido do jogo. Os resultados finais devem ser lidos junto dos logs da rodada correspondente.

Os benchmarks isolados de pintura de 600 amostras por fase, com dois processadores e heap de 256 MiB, apresentaram variação entre rodadas; não demonstram ganho uniforme de latência sobre o renderizador já otimizado anteriormente. Os ganhos comprováveis desta rodada são o funcionamento com heap menor, a eliminação de repinturas ociosas e a redução do volume de recursos de vídeo. Não se promete ausência permanente de qualquer travamento em todo hardware.

## Resultados finais

Ambiente: Windows, JDK 27, JavaFX 25.0.1 existente, tela de 1536×864. Nos ensaios restritos, a máscara de afinidade do Windows foi `0x3`, permitindo somente dois processadores lógicos, além do limite de heap de 128 MiB. A afinidade foi restaurada ao encerrar os processos de teste.

| Verificação | Resultado |
| --- | --- |
| Suíte completa, perfil padrão | Duas rodadas aprovadas, incluindo os cinco términos naturais, créditos, retorno ao menu, cancelamento, recuperação e saves antigos. Logs: `all-tests-final.log` e `all-tests-current.log`. |
| `Performance`, campanha completa e simulação | Aprovadas com heap de 128 MiB e afinidade `0x3`. Confirmação de término e código de saída zero em `performance-confirmed.log` e na execução do comando. |
| Preparação dos cinco vídeos | 50 inicializações consecutivas aprovadas no controle com áudio padronizado. As suítes posteriores também reproduziram todas as cenas. |
| Interface ociosa e entradas repetidas | Zero pedidos de pintura em 600 ms de menu ocioso. No ensaio final, 18 pedidos em 600 ms de exploração, mesmo após 100 movimentos de mouse e 100 eventos de repetição da tecla de movimento; deslocamento de 159,3 pixels. |
| Janela real, fase 1, modo leve e dois processadores lógicos | p95 de pintura: 11,027 ms; p95/p99 do intervalo do monitor EDT: 24,048/26,926 ms; maior intervalo: 52,443 ms; GC no trecho medido: 0 ms. Log: `native-low-resource.log`. O intervalo do monitor não é uma medição direta de FPS. |
| Memória do ensaio restrito | Maior pico de working set observado na campanha: aproximadamente 373 MiB; memória privada amostrada: aproximadamente 404 MiB. O heap de 128 MiB é apenas parte desse consumo. Amostras em `low-resource-affinity.json`; o código de saída desse coletor não foi capturado, por isso a confirmação de execução usa a rodada direta citada acima. |
| Seleção, simulação, pulo e rodapé | Regressões finais aprovadas com heap de 128 MiB. Incluem bloqueio feminino, saves antigos, saída da simulação por `Esc` durante vídeo, callbacks antigos e comparação de pixels para impedir que o rodapé cubra a apresentação. |
| Áudio físico | Menu e floresta avançaram mais de 22.050 frames no mixer real, com liberação dos canais após troca e silêncio. |
| Pacote JAR, fora das pastas de recursos | Aprovado: integridade SHA-256, extração reutilizada, recuperação de temporário removido e simulação inteira até os créditos. Log: `package-final.log`. |
| Execução visual por `run.bat --leve`, perfil isolado | Aprovada: créditos com os dois nomes confirmados, botão e teclado para voltar, celebração, término natural do vídeo final, `F8` no vídeo, apresentação, objetivos por clique, quiz, agradecimento, próximo capítulo e cancelamento. O hash do save antes/depois da simulação permaneceu igual. |

Os cinco MP4s passaram de **99.055.152 para 33.409.479 bytes**, redução de aproximadamente **66%**. Todos mantêm vídeo e áudio estéreo; a comparação do áudio decodificado e a inspeção dos fluxos confirmaram conteúdo não silencioso. A duração visual continua em 30 segundos por cena; o primeiro contêiner tem cerca de 20 ms adicionais de preenchimento de áudio. Os hashes anteriores e atuais estão em `out/performance/final-video-verification.json`.

Os logs aprovados ainda contêm algumas falhas nativas transitórias `UNKNOWN / ERROR_MEDIA_INVALID`, recuperadas automaticamente. O estado `FAILED` continua disponível com Tentar novamente, Pular e Voltar se as três tentativas não bastarem. A padronização dos arquivos e a recuperação melhoram o comportamento observado; as bibliotecas JavaFX originais foram mantidas.

### Correções na própria validação

Uma rodada perdeu o acesso ao desktop e produziu exceções assíncronas de Swing/JavaFX. O executor agora reprova qualquer exceção não tratada em outra thread, mesmo quando o processo principal retorna zero. Uma execução deliberadamente headless confirmou essa reprovação; a suíte de apresentação normal passou depois, com seis testes. Logs: `harness-headless.log` e `presentation-final.log`.

O teste legado de seleção também foi reproduzido com heap de 128 MiB e falhou ao preparar um segundo conjunto completo de imagens. Ele passou a reutilizar o mesmo `GameAssets`, como `Main` já faz, e a encerrar cada `Game` por inteiro. Nenhuma asserção de personagem, save ou vídeo pendente foi removida. A rodada final nesse limite passou.

A criação do JAR encontrou falta de espaço durante a compactação. O empacotador passou a usar heap de até 64 MiB e entradas sem compressão ZIP adicional. PNGs e MP4s já são comprimidos; o pacote fica um pouco maior, mas evita essa etapa de processamento e a descompressão externa durante a leitura. O JAR atualizado foi gerado e testado após esse ajuste. Para repetir:

```powershell
.\scripts\test-package.ps1
```

Os logs desta validação ficam em `out/performance/`, fora do Git. Não houve uma partida humana contínua dos quatro capítulos nem avaliação auditiva humana detalhada; a campanha inteira e a suspensão durante vídeo foram verificadas pelos testes. Outros computadores e sistemas operacionais não foram executados.

## Arquivos principais

| Arquivo ou pasta | Alteração |
| --- | --- |
| `core/Game.java` | Encerramento, créditos, simulação isolada, pintura por demanda e colisões sem alocações repetidas. |
| `core/GameAssets.java`, `assets/AssetManager.java` | Preparação das imagens pequenas e reutilização de recursos extraídos. |
| `audio/CinematicManager.java`, `audio/CinematicCatalog.java` | Sequência final, preparação, tentativas e liberação do indicador de carregamento. |
| `model/GameCredits.java` | Nomes confirmados dos criadores. |
| `Videos/` | Cinco MP4s preparados e associação de `ending` no catálogo. |
| `run.bat`, `.vscode/launch.json` | Perfil padrão e modo leve, mantendo os SDKs atuais. |
| `scripts/`, `src/test/java/` | Preparação opcional, empacotamento e regressões. |
| `docs/` | Guias atuais com índice; registros anteriores em `historico/`. A estrutura vazia antiga foi preservada em `out/backups/`. |
