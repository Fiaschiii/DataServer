package com.dataserver.dataserver;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Converte erros do cliente em respostas 400/401/404/429 com mensagens claras,
// no formato: {"status": 400, "erro": "...", "campos": {"campo": "mensagem"}}
@RestControllerAdvice
public class TratadorDeErros {

    // Corpo do POST com campos inválidos (ex.: sem preço, preço negativo, nome vazio)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> corpoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return resposta("Dados inválidos", campos);
    }

    // Parâmetros da URL fora dos limites (ex.: page=-1, size=0, size=1000)
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> parametroInvalido(HandlerMethodValidationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> {
            String nome = r.getMethodParameter().getParameterName();
            r.getResolvableErrors().forEach(e -> campos.putIfAbsent(nome, e.getDefaultMessage()));
        });
        return resposta("Parâmetros inválidos", campos);
    }

    // Parâmetro com tipo errado (ex.: page=abc, /produtos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta("Parâmetros inválidos",
            Map.of(ex.getName(), "Valor inválido: '" + ex.getValue() + "'"));
    }

    // JSON malformado ou com tipo errado (ex.: "preco": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> jsonInvalido(HttpMessageNotReadableException ex) {
        return resposta("JSON inválido ou malformado", Map.of());
    }

    // GET/PUT/DELETE /produtos/{id} com id que não existe
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> naoEncontrado(ProdutoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // POST /auth/login com e-mail inexistente ou senha errada
    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> credenciaisInvalidas(CredenciaisInvalidasException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    // POST /auth/login depois de erros demais seguidos
    @ExceptionHandler(MuitasTentativasException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> muitasTentativas(MuitasTentativasException ex) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), Map.of());
    }

    private Map<String, Object> resposta(String erro, Map<String, String> campos) {
        return resposta(HttpStatus.BAD_REQUEST, erro, campos);
    }

    private Map<String, Object> resposta(HttpStatus status, String erro, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status.value());
        corpo.put("erro", erro);
        corpo.put("campos", campos);
        return corpo;
    }
}
