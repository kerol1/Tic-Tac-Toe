package com.tictactoe.engine.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    @Test
    void newGameStartsEmptyWithXToMove() {
        Game game = Game.create(UUID.randomUUID());

        assertThat(game.board().cells()).hasSize(9).containsOnlyNulls();
        assertThat(game.nextPlayer()).contains(Symbol.X);
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.winningLine()).isEmpty();
    }

    @Test
    void appliedMoveAlternatesTurns() {
        Game game = play(Game.create(UUID.randomUUID()), 4);

        assertThat(game.board().cell(new Position(4))).isEqualTo(Symbol.X);
        assertThat(game.nextPlayer()).contains(Symbol.O);
    }

    @Test
    void rejectsMoveOnOccupiedCell() {
        Game game = play(Game.create(UUID.randomUUID()), 4);

        MoveResult result = game.apply(new Move(Symbol.O, new Position(4)));

        assertThat(result).isEqualTo(new MoveResult.Rejected(RejectionReason.CELL_OCCUPIED));
    }

    @Test
    void rejectsMoveOutOfTurn() {
        Game game = Game.create(UUID.randomUUID());

        MoveResult oFirst = game.apply(new Move(Symbol.O, new Position(0)));
        MoveResult xTwice = play(game, 0).apply(new Move(Symbol.X, new Position(1)));

        assertThat(oFirst).isEqualTo(new MoveResult.Rejected(RejectionReason.WRONG_TURN));
        assertThat(xTwice).isEqualTo(new MoveResult.Rejected(RejectionReason.WRONG_TURN));
    }

    @Test
    void winEndsTheGameAndClearsNextPlayer() {
        Game game = play(Game.create(UUID.randomUUID()), 0, 3, 1, 4, 2);

        assertThat(game.status()).isEqualTo(GameStatus.X_WON);
        assertThat(game.winningLine()).map(WinningLine::indexes).contains(List.of(0, 1, 2));
        assertThat(game.nextPlayer()).isEmpty();
    }

    @Test
    void rejectsMoveAfterWin() {
        Game finished = play(Game.create(UUID.randomUUID()), 0, 3, 1, 4, 2);

        MoveResult result = finished.apply(new Move(Symbol.O, new Position(8)));

        assertThat(result).isEqualTo(new MoveResult.Rejected(RejectionReason.GAME_FINISHED));
    }

    @Test
    void fullBoardWithoutWinnerIsADraw() {
        Game game = play(Game.create(UUID.randomUUID()), 0, 1, 2, 4, 3, 5, 7, 6, 8);

        assertThat(game.status()).isEqualTo(GameStatus.DRAW);
        assertThat(game.winningLine()).isEmpty();
        assertThat(game.nextPlayer()).isEmpty();
        assertThat(game.apply(new Move(Symbol.X, new Position(0))))
                .isEqualTo(new MoveResult.Rejected(RejectionReason.GAME_FINISHED));
    }

    @Test
    void winOnTheNinthMoveIsAWinNotADraw() {
        Game game = play(Game.create(UUID.randomUUID()), 0, 1, 2, 4, 3, 5, 7, 8, 6);

        assertThat(game.board().isFull()).isTrue();
        assertThat(game.status()).isEqualTo(GameStatus.X_WON);
        assertThat(game.winningLine()).map(WinningLine::indexes).contains(List.of(0, 3, 6));
    }

    @Test
    void positionOutsideTheBoardIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new Position(9)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Position(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boardMustHaveNineCells() {
        assertThatThrownBy(() -> new Board(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    /** Plays positions alternately, X first. */
    private static Game play(Game game, int... positions) {
        Game current = game;
        Symbol player = Symbol.X;
        for (int position : positions) {
            MoveResult result = current.apply(new Move(player, new Position(position)));
            assertThat(result).isInstanceOf(MoveResult.Applied.class);
            current = ((MoveResult.Applied) result).game();
            player = player.opponent();
        }
        return current;
    }
}
