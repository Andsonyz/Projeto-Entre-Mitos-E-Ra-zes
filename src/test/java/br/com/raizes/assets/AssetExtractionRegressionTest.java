package br.com.raizes.assets;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Run from a folder without assets, with the distribution JAR on the classpath. */
public final class AssetExtractionRegressionTest {
    public static void main(String[] args) throws Exception {
        String path = "Videos/Cenas/CenaFase1.mp4";
        var resource = AssetManager.class.getClassLoader().getResource(path);
        check(resource != null && resource.getProtocol().equals("jar"), "O teste exige o JAR empacotado.");
        var first = AssetManager.optionalFile(path);
        var second = AssetManager.optionalFile(path);
        check(first != null && first.isFile() && first.equals(second), "O recurso não foi reutilizado.");
        try (var stream = Files.newInputStream(first.toPath())) {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) digest.update(buffer,0,count);
            check(args.length == 1 && HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(args[0]), "Conteúdo extraído divergente.");
        }
        check(first.delete(), "Não foi possível liberar o temporário de teste.");
        var recovered = AssetManager.optionalFile(path);
        check(recovered != null && recovered.isFile() && !recovered.equals(first), "Cache não recuperou arquivo temporário removido.");
        System.out.println("OK: JAR, integridade, extração única e recuperação do cache.");
    }
    private static void check(boolean value,String message) {
        if (!value) throw new AssertionError(message);
    }
}
