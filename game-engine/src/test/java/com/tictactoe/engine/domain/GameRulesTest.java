package com.tictactoe.engine.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GameRulesTest {

    static Stream<List<Integer>> winningLines() {
        return Stream.of(
                List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8),
                List.of(0, 3, 6), List.of(1, 4, 7), List.of(2, 5, 8),
                List.of(0, 4, 8), List.of(2, 4, 6));
    }

    @ParameterizedTest
    @MethodSource("winningLines")
    void detectsEveryWinningLineForX(List<Integer> line) {
        Board board = boardWith(Symbol.X, line);

        assertThat(GameRules.evaluate(board, Symbol.X)).isEqualTo(GameStatus.X_WON);
        assertThat(GameRules.findWinningLine(board, Symbol.X))
                .map(WinningLine::indexes)
                .contains(line);
    }

    @ParameterizedTest
    @MethodSource("winningLines")
    void detectsEveryWinningLineForO(List<Integer> line) {
        Board board = boardWith(Symbol.O, line);

        assertThat(GameRules.evaluate(board, Symbol.O)).isEqualTo(GameStatus.O_WON);
    }

    @ParameterizedTest
    @MethodSource("winningLines")
    void aLineOfTheOtherSymbolIsNotAWin(List<Integer> line) {
        Board board = boardWith(Symbol.O, line);

        assertThat(GameRules.findWinningLine(board, Symbol.X)).isEmpty();
    }

    private static Board boardWith(Symbol symbol, List<Integer> positions) {
        Board board = Board.empty();
        for (int index : positions) {
            board = board.withMove(new Position(index), symbol);
        }
        return board;
    }
}
