package io.av360.eventdings.dispatcher.rabbit;

import com.rabbitmq.stream.*;
import io.av360.eventdings.dispatcher.Config;
import io.av360.eventdings.dispatcher.Dispatcher;
import io.av360.eventdings.lib.CloudEventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RabbitMQStream {
    private static RabbitMQStream instance;

    Environment environment;

    private static final Logger log = LoggerFactory.getLogger(RabbitMQStream.class);
    private Consumer consumer;

    private RabbitMQStream() {
    }

    public static RabbitMQStream getInstance() {
        if (instance == null) {
            instance = new RabbitMQStream();
            instance.init();
        }
        return instance;
    }

    public void init() {
        Config cfg = Config.getInstance();

        environment = Environment.builder()
                .host(cfg.host())
                .username(cfg.user())
                .password(cfg.password())
                .port(cfg.streamPort())
                .virtualHost(cfg.virtualHost())
                .build();

        this.consumer = this.environment.consumerBuilder()
                .stream(Config.getInstance().stream())
                .offset(OffsetSpecification.first())
                .messageHandler((offset, message) -> {
                    //TODO: This is a hack to remove the "Data{" and "}" from the message
                    String msg = message.getBody().toString();
                    if (msg.startsWith("Data{")) {
                        msg = msg.substring(5, msg.length() - 1);
                    }

                    if (CloudEventValidator.isValidCloudEvent(msg)) {
                        Dispatcher.dispatch(msg);
                    }
                })
                .build();

    }

    public void close() throws Exception {
        this.consumer.close();
        this.environment.close();
    }
}