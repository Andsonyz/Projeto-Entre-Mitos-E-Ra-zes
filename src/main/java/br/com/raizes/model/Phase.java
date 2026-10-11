package br.com.raizes.model;

public enum Phase {
    CURUPIRA(
        "FASE 1 - OS RASTROS DA CA\u00C7A", "Curupira", "Ca\u00E7a ilegal",
        "Guardi\u00E3o de cabelos flamejantes e p\u00E9s voltados para tr\u00E1s, protege os animais e os segredos da mata. Com assobios e rastros invertidos, confunde ca\u00E7adores e ensina que cada esp\u00E9cie tem seu lugar no equil\u00EDbrio da floresta.",
        "Image/Personagens/guardioes/CURUPIRA.PNG"),
    SACI(
        "FASE 2 - A TRAVESSIA DO SACI", "Saci", "Responsabilidade ambiental",
        "Menino de uma perna s\u00F3 e dono de um gorro vermelho, o Saci cruza a mata em redemoinhos. Brincalh\u00E3o e observador, desafia o viajante a pensar nas consequ\u00EAncias de suas escolhas e a cuidar dos caminhos, da \u00E1gua e dos seres da floresta.",
        "Image/Personagens/guardioes/SACI.png"),
    CAIPORA(
        "FASE 3 - ENTRE CINZAS E RA\u00CDZES", "Caipora", "Queimadas",
        "Protetora incans\u00E1vel dos animais e da mata, a Caipora conhece trilhas escondidas e percebe qualquer amea\u00E7a \u00E0 vida silvestre. Sua coragem inspira o cuidado com a floresta e lembra que o fogo pode destruir, em pouco tempo, muitos lares naturais.",
        "Image/Personagens/guardioes/CAIPORA.png"),
    NEGRINHO(
        "FASE 4 - OS RASTROS DO LIXO", "Negrinho do Pastoreio", "Polui\u00E7\u00E3o e descarte",
        "Figura do folclore ga\u00FAcho, o Negrinho do Pastoreio \u00E9 lembrado por sua liga\u00E7\u00E3o com as estrelas e por ajudar a encontrar o que se perdeu. Nesta jornada, sua perseveran\u00E7a guia a busca por res\u00EDduos e mostra como pequenas atitudes ajudam a recuperar a floresta.",
        "Image/Personagens/guardioes/NEGRINHODOPASTOREIO.png");

    public final String title, guardian, theme, legend, portrait;

    Phase(String title, String guardian, String theme, String legend, String portrait) {
        this.title = title;
        this.guardian = guardian;
        this.theme = theme;
        this.legend = legend;
        this.portrait = portrait;
    }
}
