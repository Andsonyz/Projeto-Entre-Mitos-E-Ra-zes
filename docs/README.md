# Documentação

| Guia | Conteúdo |
| --- | --- |
| [README do projeto](../README.md) | Execução, controles, campanha, créditos e simulação. |
| [Vídeos](VIDEOS.md) | Catálogo de cenas, encerramento, formatos e recuperação. |
| [Personagens](PERSONAGENS.md) | Sprites, animações, seleção e compatibilidade de saves. |
| [Plataformas](PLATAFORMAS.md) | Arte ativa e alinhamento com a colisão. |
| [Desempenho](DESEMPENHO.md) | Perfis de execução, otimizações, testes e limitações. |

## Histórico

Os arquivos de `historico/` registram decisões e verificações anteriores. Podem mencionar caminhos ou problemas já substituídos. Para executar o jogo, use os guias acima.

- [Revisão inicial](historico/revisao-inicial.md)
- [Planejamento dos personagens e vídeos](historico/planejamento-personagens.md)
- [Validação dos personagens](historico/validacao-personagens.md)
- [Atualizações de apresentação e jogabilidade](historico/atualizacoes-2026-11.md)

## Pastas do projeto

`src/main/java` contém o jogo; `src/test/java`, as regressões e probes de diagnóstico. Imagens, áudio e vídeos permanecem em `Image/`, `Audio/` e `Videos/`, sem cópias ativas em outros locais. `scripts/` reúne compilação, testes e preparação opcional de vídeos. `lib/` mantém os SDKs existentes. `out/` é uma pasta ignorada pelo Git, exclusiva para builds, logs, capturas e backups de trabalho.
