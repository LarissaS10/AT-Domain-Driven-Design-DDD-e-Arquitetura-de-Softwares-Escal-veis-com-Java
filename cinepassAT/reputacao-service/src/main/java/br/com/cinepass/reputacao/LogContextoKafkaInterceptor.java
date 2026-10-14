package br.com.cinepass.reputacao;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class LogContextoKafkaInterceptor implements RecordInterceptor<Object, Object> {
    private static final Logger log = LoggerFactory.getLogger(LogContextoKafkaInterceptor.class);

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        String reservaId = record.key() == null ? null : record.key().toString();
        String correlationId = cabecalho(record, "correlationId");
        colocar("eventId", cabecalho(record, "eventId"));
        colocar("correlationId", correlationId != null ? correlationId : reservaId);
        colocar("reservaId", reservaId);
        log.info("Mensagem recebida: topico={} particao={} offset={} tipo={}",
                record.topic(), record.partition(), record.offset(), cabecalho(record, "eventType"));
        return record;
    }

    @Override
    public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        log.info("Processamento concluído: topico={} particao={} offset={} tipo={}",
                record.topic(), record.partition(), record.offset(), cabecalho(record, "eventType"));
    }

    @Override
    public void failure(ConsumerRecord<Object, Object> record, Exception exception, Consumer<Object, Object> consumer) {
        log.error("Falha no processamento: topico={} particao={} offset={} tipo={} erro={}",
                record.topic(), record.partition(), record.offset(), cabecalho(record, "eventType"),
                exception.getClass().getSimpleName());
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        MDC.remove("eventId");
        MDC.remove("correlationId");
        MDC.remove("reservaId");
    }

    private static String cabecalho(ConsumerRecord<Object, Object> record, String nome) {
        Header header = record.headers().lastHeader(nome);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static void colocar(String chave, String valor) {
        if (valor != null) {
            MDC.put(chave, valor);
        }
    }
}
