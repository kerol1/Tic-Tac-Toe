package com.tictactoe.engine.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable 3x3 board. Cells are indexed 0..8; an empty cell is {@code null}.
 */
public record Board(List<Symbol> cells) {

    public static final int SIZE = 9;

    public Board {
        Objects.requireNonNull(cells, "cells");
        if (cells.size() != SIZE) {
            throw new IllegalArgumentException("Board must have exactly " + SIZE + " cells");
        }
        // not List.copyOf: it rejects the nulls that stand for empty cells
        cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public static Board empty() {
        return new Board(Collections.nCopies(SIZE, null));
    }

    public Symbol cell(Position position) {
        return cells.get(position.index());
    }

    public boolean isFree(Position position) {
        return cell(position) == null;
    }

    public boolean isFull() {
        return cells.stream().noneMatch(Objects::isNull);
    }

    public Board withMove(Position position, Symbol symbol) {
        List<Symbol> updated = new ArrayList<>(cells);
        updated.set(position.index(), symbol);
        return new Board(updated);
    }
}
