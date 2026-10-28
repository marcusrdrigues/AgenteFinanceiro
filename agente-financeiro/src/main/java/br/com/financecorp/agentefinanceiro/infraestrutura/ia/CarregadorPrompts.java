package br.com.financecorp.agentefinanceiro.infraestrutura.ia;

import br.com.financecorp.agentefinanceiro.infraestrutura.config.PropriedadesIa;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Carrega {@code prompts/<nome>.<papel>.<versao>.st} pela versão configurada em
 * {@code financeiro.ia.versao-prompts}. Arquivo ausente falha na subida, não na primeira pergunta.
 */
@Component
public final class CarregadorPrompts {

    private final ResourceLoader carregador;
    private final Map<String, String> versoes;

    public CarregadorPrompts(ResourceLoader carregador, PropriedadesIa propriedades) {
        this.carregador = carregador;
        this.versoes = propriedades.versaoPrompts();
    }

    public Resource system(String nome) {
        return carregar(nome, "system");
    }

    public Resource user(String nome) {
        return carregar(nome, "user");
    }

    public String versao(String nome) {
        String versao = versoes.get(nome);
        if (versao == null) {
            throw new IllegalStateException("Versão do prompt '" + nome + "' não configurada em financeiro.ia.versao-prompts.");
        }
        return versao;
    }

    private Resource carregar(String nome, String papel) {
        String caminho = "prompts/" + nome + "." + papel + "." + versao(nome) + ".st";
        Resource recurso = carregador.getResource("classpath:" + caminho);
        if (!recurso.exists()) {
            throw new IllegalStateException("Prompt ausente: " + caminho);
        }
        return recurso;
    }
}
