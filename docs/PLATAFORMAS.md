# Arte e superfície das plataformas

O recurso ativo é `Image/Cenarios/Elementos/plataforma.png`. A imagem foi reconstruída com o **image_gen integrado**, usando a plataforma anterior como referência de madeira, musgo, raízes e estilo pixel art. A cópia de comparação fica em `out/updates-gameplay/plataforma-anterior.png`, fora da distribuição.

O PNG gerado tem **1774 × 887 pixels**, proporção 2:1. A conferência de todos os pixels encontrou **zero pixels não opacos**: a textura ocupa toda a imagem, sem margem transparente. SHA-256: `39729B8FDE531B8E43E93F18A9392A00E8E94BFED495A57C488A2135E25A0D74`.

`GameAssets` prepara a textura em **800 × 400** uma vez. Para cada tamanho de plataforma, o jogo gera uma imagem com a largura e a altura exatas do retângulo físico. A chave do cache inclui ambas as medidas. A adaptação retangular da textura é intencional; não se trata de um personagem que precisa manter uma silhueta proporcional.

A superfície de apoio passou a ser `platform.y`, coincidente com a primeira linha visível da arte. O deslocamento antigo de 9 pixels foi removido. As 39 plataformas dos quatro capítulos foram verificadas: arte sem vazios nas bordas, dimensões coincidentes com os retângulos e pouso com os pés sobre a superfície. A hitbox do jogador permanece em 42 × 58 pixels e seu desenho em 58 × 78.

## Prompt usado

O resultado foi copiado do diretório de imagens geradas para o recurso ativo do projeto. O prompt abaixo permite gerar outra versão, mas não promete uma reprodução idêntica dos pixels:

```text
Use case: style-transfer. Asset type: PNG texture for the walkable platforms of the Java pixel-art forest game Entre Mitos e Raizes. Reference image: the local plataforma.png is a style reference only, especially the brown bark, wood plank top, moss and roots. Primary request: reconstruct a clean rectangular platform texture which fills 100 percent of the canvas, with NO empty padding. Wide 2:1 canvas, ideally 800 by 400 pixels. The entire top edge is a perfectly horizontal continuous walkable plank surface, beginning at pixel y=0; the wood front extends fully to the bottom edge. The wood must extend to both left and right edges, all four corners filled. Restrained moss along the very top and root/bark details in the front. Match the existing warm brown and olive-green pixel-art style. This is a flat side-view game texture, not an illustration of a floating object. No background scenery, no text, no character, no outer shadow, no transparent holes, no border or empty margins. Fill every pixel of the rectangular image with the platform material. It will be resized into shallow platform collision rectangles; keep all important details simple and horizontal.
```
