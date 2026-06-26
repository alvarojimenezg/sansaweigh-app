package cl.sansaweigh.sansaweighapp.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración de la conexión a Redis usando el cliente Jedis.
 * - JedisConnectionFactory: abre la conexión al servidor Redis (host/port de application.properties).
 * - RedisTemplate: herramienta para leer/escribir en Redis; las claves se guardan como texto
 *   y los valores como JSON, para que sean legibles desde RedisInsight/redis-cli.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // Claves como texto legible (en RedisInsight/redis-cli). Los valores usan el
        // serializador por defecto; la entidad @RedisHash se guarda como hash campo a campo,
        // así que no depende de este serializador.
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        return template;
    }
}
