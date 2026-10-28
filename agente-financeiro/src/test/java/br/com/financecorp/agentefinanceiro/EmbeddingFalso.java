package br.com.financecorp.agentefinanceiro;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Embedding determinístico para testes (testes-ia §4): saco de palavras com hash em 1536 posições,
 * normalizado. Textos com palavras em comum ficam próximos; nenhum teste chama o provedor real.
 * {@link #falharNaChamada(int)} simula o provedor caindo no meio de uma ingestão.
 */
public final class EmbeddingFalso implements EmbeddingModel {

    public static final int DIMENSOES = 1536;

    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile int falharNaChamada = -1;

    public void falharNaChamada(int numero) {
        chamadas.set(0);
        falharNaChamada = numero;
    }

    public void restaurar() {
        falharNaChamada = -1;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest requisicao) {
        if (chamadas.incrementAndGet() == falharNaChamada) {
            throw new IllegalStateException("embedding falso fora do ar");
        }
        List<Embedding> vetores = new ArrayList<>();
        List<String> textos = requisicao.getInstructions();
        for (int i = 0; i < textos.size(); i++) {
            vetores.add(new Embedding(vetor(textos.get(i)), i));
        }
        return new EmbeddingResponse(vetores);
    }

    @Override
    public float[] embed(Document documento) {
        return vetor(documento.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSOES;
    }

    public static float[] vetor(String texto) {
        float[] v = new float[DIMENSOES];
        String limpo = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (String palavra : limpo.split("[^a-z0-9]+")) {
            if (palavra.length() > 3) {
                v[Math.floorMod(palavra.hashCode(), DIMENSOES)] += 1f;
            }
        }
        double norma = 0;
        for (float x : v) {
            norma += x * x;
        }
        if (norma == 0) {
            v[0] = 1f;
            return v;
        }
        float inverso = (float) (1 / Math.sqrt(norma));
        for (int i = 0; i < v.length; i++) {
            v[i] *= inverso;
        }
        return v;
    }
}
