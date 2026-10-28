package br.com.financecorp.agentefinanceiro.interfaces.web;

import br.com.financecorp.agentefinanceiro.aplicacao.servico.ResponderPerguntaService;
import br.com.financecorp.agentefinanceiro.interfaces.web.dto.PerguntaRequest;
import br.com.financecorp.agentefinanceiro.interfaces.web.dto.RespostaPerguntaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só traduz HTTP: valida a forma, chama o caso de uso e devolve o DTO. */
@RestController
@RequestMapping("/api/v1/conversas")
@Tag(name = "Perguntas", description = "Perguntas ao assistente financeiro da FinanceCorp")
public class PerguntaController {

    private final ResponderPerguntaService servico;

    public PerguntaController(ResponderPerguntaService servico) {
        this.servico = servico;
    }

    @PostMapping(path = "/{conversaId}/perguntas", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Envia uma pergunta numa conversa",
            description = "Cria a conversa se o identificador for novo. Situações como \"sem informação\" e "
                    + "\"fora do domínio\" respondem 200 com o campo ausencias preenchido.")
    @ApiResponse(responseCode = "200", description = "Resposta do assistente")
    @ApiResponse(responseCode = "400", description = "conversaId fora do formato UUID v4 ou corpo inválido",
            content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "413", description = "Pergunta acima de 1.000 caracteres",
            content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "429", description = "Limite de requisições atingido (cabeçalho Retry-After)",
            content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "503", description = "Provedor do modelo indisponível (cabeçalho Retry-After)",
            content = @Content(mediaType = "application/problem+json"))
    public RespostaPerguntaDto perguntar(
            @Parameter(description = "UUID versão 4 gerado pelo cliente",
                    schema = @Schema(format = "uuid", example = "3f0b8a3e-6a1c-4c7e-9a52-0c8f2d4b9e11"))
            @PathVariable String conversaId,
            @Valid @RequestBody PerguntaRequest corpo) {
        return RespostaPerguntaDto.de(servico.responder(conversaId, corpo.pergunta(), MDC.get(FiltroTraceId.CHAVE_MDC)));
    }
}
