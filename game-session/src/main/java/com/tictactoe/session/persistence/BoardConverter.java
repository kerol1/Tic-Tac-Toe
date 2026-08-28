package com.tictactoe.session.persistence;

import com.tictactoe.session.domain.Board;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class BoardConverter implements AttributeConverter<Board, String> {

    @Override
    public String convertToDatabaseColumn(Board board) {
        return board.encode();
    }

    @Override
    public Board convertToEntityAttribute(String encoded) {
        return Board.decode(encoded);
    }
}
