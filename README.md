# Entre Mitos e Raízes

Jogo 2D de plataforma lateral feito em Java Swing, com reprodução de vídeo pelo JavaFX Media.

## Requisitos

- Windows e JDK 25 instalado e disponível no `PATH` (`java` e `javac`).
- Bibliotecas JavaFX 25 para Windows em `lib/javafx/`.
- Pastas `Image/` e `Videos/` na raiz do projeto. As imagens adicionais da interface ficam em `src/main/resources/`.

## Executar

Execute `run.bat`. O script compila automaticamente todos os arquivos Java, copia imagens e vídeos para `out/` e inicia o jogo. Os arquivos de saída em `out/` podem ser recriados a qualquer momento.

Ao distribuir o projeto, inclua `src/`, `Image/`, `Videos/`, `lib/javafx/`, `run.bat` e este README. Os saves são criados fora do projeto em `%USERPROFILE%\.entremitoseraizes\saves`.

## Controles

- `A/D` ou setas: correr
- `W`, seta para cima ou `Espaço`: pular
- `Enter`: confirmar e avançar diálogos
- `M`: abrir ou fechar o mapa
- `F5`: salvar no slot atual
- `Esc`: pausar ou voltar
- `Delete`: excluir o slot selecionado

## Sistemas

A campanha tem quatro fases, cada uma com introdução, retrato e lenda do guardião, objetivo próprio, diálogo educativo e desafio:

1. Os Rastros da Caça — Curupira: encontre sementes e desative armadilhas.
2. A Travessia do Saci — Saci: atravesse plataformas e vãos.
3. Entre Cinzas e Raízes — Caipora: encha o balde nas fontes e apague os focos com `E`.
4. Os Rastros do Lixo — Negrinho do Pastoreio: recolha resíduos e conclua a campanha.

O jogo também inclui dois slots locais, continuar campanha, apelido, pausa, mapa de progresso, configurações, controles, extras e quiz.

## Prólogo em vídeo

Ao criar uma campanha, as cenas são reproduzidas dentro da janela, com narração na parte inferior. Cada cena avança quando o vídeo termina; `Enter` ou `Espaço` avança manualmente e `Esc` pula o prólogo.

- `Videos/cena1.mp4` e `Videos/cena2.mp4` estão incluídos.
- `Videos/cena3.mp4` e `Videos/cena4.mp4` são opcionais e entram automaticamente na sequência quando adicionados.
