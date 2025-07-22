package com.file.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class SecurityContextRedisSerializer implements RedisSerializer<Object> {
    private final Jackson2JsonRedisSerializer<Object> serializer;

    public SecurityContextRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        this.serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        return serializer.serialize(object);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        return serializer.deserialize(bytes);
    }
}