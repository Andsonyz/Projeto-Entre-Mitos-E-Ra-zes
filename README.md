# Entre Mitos e Raizes

Jogo 2D de plataforma lateral em Java Swing, feito para o JDK 27 e inspirado em jogos classicos de plataforma.

## Executar

No Windows, execute `run.bat`. O script compila o projeto e abre a janela do jogo.

## Controles

- `A/D` ou setas: correr
- `W`, seta para cima ou `Espaco`: pular
- `Enter`: confirmar e avancar dialogos
- `M`: abrir mapa
- `F5`: salvar no slot atual
- `Esc`: pausa ou voltar
- `Delete`: apagar o slot selecionado

## Sistemas

Campanha linear de quatro fases, cada uma com introducao, retrato e lenda do guardiao, objetivo proprio, dialogo educativo, desafio e desbloqueio da fase seguinte:

1. Os Rastros da Caca - Curupira: investigue a trilha e encontre sementes.
2. A Travessia do Saci - Saci: atravesse plataformas e vãos.
3. Entre Cinzas e Raizes - Caipora: encha o balde nas fontes e apague os focos com `E`.
4. Os Rastros do Lixo - Negrinho do Pastoreio: recolha residuos e conclua a campanha.

O jogo inclui menu inicial, novo jogo, dois slots locais, continuar, apelido, salvamento do progresso da campanha, pausa, mapa com fases bloqueadas/desbloqueadas/concluidas, configuracoes de tamanho de texto e alto contraste, tela de controles, extras, quiz e desafios de plataforma.

Os saves ficam fora do projeto em `%USERPROFILE%\\.entremitoseraizes\\saves`.
