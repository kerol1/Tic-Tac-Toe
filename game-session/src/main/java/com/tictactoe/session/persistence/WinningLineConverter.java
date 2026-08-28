package com.tictactoe.session.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Three positions as {@code "0,4,8"}; null when there is no winner. */
@Converter
class WinningLineConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> line) {
        return line == null ? null : line.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String encoded) {
        return encoded == null ? null : Arrays.stream(encoded.split(",")).map(Integer::valueOf).toList();
    }
}
