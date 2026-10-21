# Personagens e animações

O catálogo está em `src/main/java/br/com/raizes/character/CharacterDefinition.java`. Ele centraliza ID do save, nome, disponibilidade, folhas, intervalos de quadros e duração de cada quadro. O masculino está disponível; o feminino aparece como **Em breve**, não possui recursos associados e é rejeitado também pela confirmação interna e pelo carregador de sprites.

## Recursos utilizados

Origem: `C:\Users\anton\TestesRaizes\testesJAVA\testes.zip`, extraído e inspecionado em uma pasta temporária antes da cópia. Nenhum programa ou script do arquivo foi executado. `testes.java` foi apenas lido como referência, sem incorporá-lo ao projeto.

Somente os quatro PNGs de `Personagem/` foram copiados para `Image/Personagens/AventureiroMasculino/`. Os arquivos antigos de Greg continuam no disco; o código de produção não os carrega.

| Arquivo | Dimensões | Organização e uso | Tempo por quadro |
| --- | --- | --- | --- |
| `PersonagemParado.png` | 1254 × 1254 | 4 colunas, 4 faixas; 16 poses parado | 180 ms |
| `PersonagemAndando.png` | 1254 × 1254 | 4 colunas, 4 faixas; 16 poses andando | 110 ms |
| `PersonagemCorrendo.png` | 1254 × 1254 | 4 colunas, 4 faixas; 16 poses correndo | 70 ms |
| `PersonagemPulando.png` | 1536 × 1024 | 4 colunas, 2 faixas; 4 poses de subida e 4 de queda/pouso | 100 ms |

Todas são RGBA com transparência, voltadas para a direita. A ordem é por linha, da esquerda para a direita. O espelhamento acontece somente ao olhar para a esquerda. Não existe arquivo separado de queda: as quatro últimas poses da folha de salto são usadas como alternativa; subida e queda seguram a última pose até mudar de ação.

### Recorte e desenho

As folhas não têm células verticais regulares. Em particular, 1254 não é divisível por quatro e as poses de salto atravessam a divisão central de uma grade uniforme. `SpriteAnimationSet` encontra as faixas ocupadas **separadamente em cada coluna**, com alpha maior que 16 e margem de dois pixels. Se a contagem de faixas não corresponder ao catálogo, ele informa recorte ambíguo em vez de cortar silenciosamente.

O processamento é feito uma vez por personagem e fica em cache. A escala de origem considera a largura nominal da coluna; todos os quadros recebem um canvas comum, com a mesma âncora horizontal e a base dos pés alinhada. O desenho usa uma única escala proporcional e a mesma âncora ao trocar de ação, sem alterar posição física ou colisões.

Este projeto utiliza hitbox **42 × 58**, preservada, e envelope de desenho **58 × 78**. As medidas 40 × 80 e 64 × 96 do protótipo fornecido não foram aplicadas a este projeto. `PlayerAnimator` usa segundos acumulados, independentemente de quantas vezes a imagem é desenhada. Manter a mesma ação não reinicia a animação. `Shift` permite andar; o movimento padrão continua sendo corrida.

## Seleção e saves

Fluxo de novo jogo: **slot → nome → seleção → prólogo → capítulo → apresentação da fase**. Setas ou A/D selecionam, Enter/Espaço confirmam, Esc volta ao nome; o mouse segue os cartões e botões. O feminino pode receber destaque para explicar o bloqueio, mas não pode ser confirmado por nenhum desses caminhos.

A propriedade `character=male` usa o arquivo de save existente. Campo ausente, ID desconhecido ou personagem indisponível adota o masculino. Não há leitura de imagens femininas nem substituição visual pelo masculino no cartão feminino.

Para disponibilizar outro personagem futuramente, adicione seus recursos e mapeamento no catálogo, habilite sua disponibilidade e valide os recortes/poses com os testes. O carregador atual espera faixas transparentes separadas por coluna; folhas de outro formato podem exigir um recortador próprio.

## Integridade

SHA-256 do ZIP: `b78821492f0e1fb3ad609a122366c5dc22fb780fed8a8f4e10e997f8a37853bc`.

| PNG | SHA-256 conferido entre ZIP e destino |
| --- | --- |
| Andando | `fdf7decfdf7b4918e0a941ea770de49a09d7213798a211481a683530996dcd21` |
| Correndo | `90afbfad92d36a431c665b4852cfce7441dc33d49aaf7856d608130cad24d19a` |
| Parado | `e303bc246fc400daabd596edb75660d568dcc3b59fe8fe1564da0804ade7751e` |
| Pulando | `0bb257a92a8b1d98588b2cddd04ca6a78f844668feee3c997686838a8ec1c4e1` |

Os testes geram a folha de contato `out/project-tests/artifacts/sprites-contact.png`, os cartões de seleção e imagens da fase. Esses artefatos permitem conferir os quadros e o layout; não substituem a observação interativa de toda a animação em janela.
