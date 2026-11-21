package br.com.raizes.quiz;

import br.com.raizes.model.Question;

/** Comparable distractors; option order is shuffled for every quiz round. */
public final class QuestionBank {
    private QuestionBank() { }
    public static final Question[][] QUESTIONS = {
        {
            q("Qual situação caracteriza a caça ilegal?", 0, "Captura proibida de animais silvestres", "Pesquisa autorizada sobre aves da região", "Observação de ninhos a distância segura", "Registro de espécies em área protegida"),
            q("Sem um predador nativo, o que pode ocorrer?", 2, "Fim de toda a competição por alimento", "Desaparecimento de qualquer ser vivo", "Crescimento de certas populações de presas", "Aumento automático de todas as plantas"),
            q("Um animal dispersor de sementes ajuda na...", 1, "Formação de rochas", "Regeneração da floresta", "Produção de nuvens", "Limpeza das estradas"),
            q("Qual ação contribui para conservar um habitat?", 3, "Trocar a mata por um gramado", "Abrir novos atalhos na mata", "Iluminar os ninhos à noite", "Manter a vegetação nativa"),
            q("Biodiversidade é a variedade de...", 2, "Estradas de uma região", "Construções nas cidades", "Seres vivos e suas relações", "Reservatórios de concreto"),
            q("Não alimentar a fauna silvestre ajuda a evitar...", 0, "Mudanças no comportamento dos animais", "A formação natural de folhas nas árvores", "A circulação do vento entre as plantas", "A produção de sementes pelas espécies"),
            q("Ao encontrar uma armadilha na trilha, qual atitude é segura?", 3, "Tentar abrir a peça com as próprias mãos", "Transportar a armadilha até outra trilha", "Esconder a peça para ninguém encontrá-la", "Manter distância e avisar a equipe local"),
            q("A perda de uma espécie pode alterar...", 1, "A posição geográfica da floresta", "As relações da cadeia alimentar", "A duração exata de todas as estações", "O sentido da rotação do planeta")
        },
        {
            q("Numa trilha sinalizada, qual escolha causa menos impacto?", 1, "Abrir atalhos entre as plantas", "Usar os caminhos demarcados", "Levar plantas como lembrança", "Deixar marcas nas árvores"),
            q("Qual prática reduz o consumo de água?", 2, "Lavar o quintal com a mangueira aberta", "Deixar a água correr antes de cada uso", "Fechar a torneira ao escovar os dentes", "Repetir a lavagem de áreas já limpas"),
            q("Qual objeto substitui vários descartáveis no dia a dia?", 0, "Garrafa reutilizável", "Copo de uso único", "Canudo embalado", "Talher descartável"),
            q("Para observar um animal na natureza, é melhor...", 2, "Tentar atraí-lo oferecendo alimento", "Cercá-lo para conseguir uma boa foto", "Respeitar sua distância e seu caminho", "Aproximar o flash para destacar os olhos"),
            q("Qual hábito evita desperdício de energia?", 1, "Ligar aparelhos sem necessidade", "Apagar luzes em cômodos vazios", "Manter a televisão ligada sem uso", "Deixar portas abertas com ar ligado"),
            q("O consumo responsável começa ao...", 0, "Planejar o que é necessário", "Comprar para acumular estoque", "Preferir sempre a maior embalagem", "Substituir objetos ainda úteis"),
            q("Por que evitar sair das trilhas demarcadas?", 3, "Para impedir a passagem de toda a fauna", "Para aumentar a coleta de plantas locais", "Para afastar os visitantes das nascentes", "Para reduzir danos ao solo e à vegetação"),
            q("Uma atividade coletiva de cuidado ambiental é...", 3, "Abrir valas para esconder embalagens", "Plantar qualquer espécie sem orientação", "Retirar placas para ampliar o caminho", "Organizar a coleta segura de resíduos")
        },
        {
            q("Qual combinação favorece a propagação do fogo?", 0, "Vegetação seca e vento", "Solo úmido e chuva", "Névoa e mata encharcada", "Chuva e baixa temperatura"),
            q("Ao perceber um incêndio, a prioridade é...", 1, "Entrar na fumaça para procurar a origem", "Ir para um local seguro e pedir ajuda", "Chegar mais perto para registrar imagens", "Atravessar as chamas pelo trecho curto"),
            q("Uma consequência das queimadas é a...", 2, "Recuperação imediata de todos os ninhos", "Eliminação permanente de todo o calor", "Perda de habitats e de seres vivos", "Criação instantânea de novas nascentes"),
            q("Que material pode iniciar fogo na vegetação seca?", 3, "Folha molhada", "Pedra fria", "Terra úmida", "Bituca acesa"),
            q("Queimar lixo ao ar livre pode produzir...", 0, "Fumaça nociva e risco de incêndio", "Ar limpo sem qualquer partícula", "Adubo pronto para todas as culturas", "Água própria para consumo imediato"),
            q("A vegetação nativa protege o solo ao...", 3, "Eliminar toda a água que chega ao chão", "Impedir qualquer crescimento de raízes", "Aumentar o impacto direto de cada chuva", "Reduzir a erosão com cobertura e raízes"),
            q("O combate a um incêndio florestal cabe a...", 1, "Visitantes sem equipamento", "Equipes treinadas", "Crianças em passeio escolar", "Qualquer pessoa curiosa"),
            q("Após uma queimada, a recuperação de um habitat...", 2, "Ocorre inteira na mesma noite", "Dispensa a presença de plantas", "Exige tempo e condições adequadas", "Torna todas as espécies iguais")
        },
        {
            q("Qual objeto deve ir a um ponto de coleta específico?", 1, "Casca de fruta", "Pilha usada", "Folha caída", "Resto de salada"),
            q("A sequência dos 3Rs é...", 0, "Reduzir, reutilizar, reciclar", "Recolher, remover, rasgar", "Repor, regar, recolocar", "Repetir, reter, reconstruir"),
            q("Antes de reciclar uma embalagem, convém...", 3, "Enterrá-la junto de outros materiais", "Misturar o conteúdo com folhas secas", "Largá-la perto de um bueiro fechado", "Consultar a orientação da coleta local"),
            q("Uma sacola reutilizável ajuda a...", 2, "Aumentar o número de embalagens", "Misturar todos os resíduos da casa", "Reduzir o uso de descartáveis", "Impedir qualquer tipo de coleta"),
            q("Resíduos plásticos podem prejudicar animais por...", 0, "Ingestão ou aprisionamento", "Produção de alimento natural", "Criação de abrigo sempre seguro", "Aumento da qualidade da água"),
            q("Compostagem aproveita principalmente...", 3, "Pilhas e baterias", "Latas e ferramentas", "Vidros e lâmpadas", "Restos orgânicos"),
            q("Ao encontrar material cortante na trilha, é melhor...", 1, "Segurá-lo sem proteção até chegar à saída", "Evitar o contato e avisar a equipe local", "Empurrá-lo com o pé para perto do mato", "Quebrá-lo em partes para facilitar a coleta"),
            q("Reutilizar um recipiente significa...", 2, "Deixá-lo em qualquer ponto da cidade", "Transformá-lo sempre em matéria-prima", "Usá-lo novamente para uma função útil", "Misturá-lo a todos os restos da cozinha")
        }
    };
    private static Question q(String prompt, int answer, String... options) { return new Question(prompt, options, answer); }
}
