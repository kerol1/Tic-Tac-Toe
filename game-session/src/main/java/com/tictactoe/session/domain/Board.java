package com.tictactoe.session.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Immutable board, empty cells {@code null}. Live state always comes from an Engine
 * response; {@link #with} only exists to rebuild intermediate positions from the move
 * history when replaying it.
 */
public record Board(List<Player> cells) {

    public static final int SIZE = 9;
    private static final char EMPTY = '.';

    public Board {
        if (cells.size() != SIZE) {
            throw new IllegalArgumentException("Board must have exactly " + SIZE + " cells");
        }
        cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public static Board empty() {
        return new Board(Collections.nCopies(SIZE, null));
    }

    public Player cell(int position) {
        return cells.get(position);
    }

    public List<Integer> freePositions() {
        return IntStream.range(0, SIZE).filter(index -> cells.get(index) == null).boxed().toList();
    }

    public Board with(int position, Player player) {
        List<Player> updated = new ArrayList<>(cells);
        updated.set(position, player);
        return new Board(updated);
    }

    /** Compact persistence form, e.g. {@code X.O......}. */
    public String encode() {
        return cells.stream()
                .map(cell -> cell == null ? String.valueOf(EMPTY) : cell.name())
                .collect(Collectors.joining());
    }

    public static Board decode(String encoded) {
        List<Player> cells = encoded.chars()
                .mapToObj(symbol -> symbol == EMPTY ? null : Player.valueOf(String.valueOf((char) symbol)))
                .toList();
        return new Board(cells);
    }
}
