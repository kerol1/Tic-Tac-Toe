package com.tictactoe.session.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A WireMock transformer that behaves like the real Game Engine (create, read, apply
 * moves with full rules) and can be told to misbehave in precise ways: fail before or
 * after applying a move, reset the connection, stall, refuse, or go down entirely.
 */
public class FakeEngine implements ResponseDefinitionTransformerV2 {

    public static final String NAME = "fake-engine";

    public enum MoveFault {
        RESET_BEFORE_APPLY, RESET_AFTER_APPLY, DELAY_AFTER_APPLY, WRONG_TURN
    }

    private static final Pattern GAME_PATH = Pattern.compile("^/games/([0-9a-fA-F-]{36})(/moves)?$");
    private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final Deque<MoveFault> moveFaults = new ArrayDeque<>();
    private final AtomicInteger movesApplied = new AtomicInteger();
    private volatile boolean down;
    private volatile boolean failWhenGameEnds;
    private volatile boolean failCreateAfterApply;
    private volatile int holdBeforeMove;
    private volatile CountDownLatch holdGate;
    private volatile CountDownLatch holdReached;

    public synchronized void reset() {
        games.clear();
        moveFaults.clear();
        movesApplied.set(0);
        down = false;
        failWhenGameEnds = false;
        failCreateAfterApply = false;
        holdBeforeMove = 0;
        holdGate = null;
        holdReached = null;
    }

    public void goDown() {
        down = true;
    }

    public synchronized void failNextMove(MoveFault fault) {
        moveFaults.add(fault);
    }

    public void failWhenGameEnds() {
        failWhenGameEnds = true;
    }

    public void failCreateAfterApply() {
        failCreateAfterApply = true;
    }

    /** Blocks the {@code moveNumber}-th move until {@code gate} is released. */
    public void holdBeforeMove(int moveNumber, CountDownLatch gate, CountDownLatch reached) {
        holdBeforeMove = moveNumber;
        holdGate = gate;
        holdReached = reached;
    }

    public int movesApplied() {
        return movesApplied.get();
    }

    public String boardOf(String gameId) {
        return games.get(gameId).board();
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        Request request = serveEvent.getRequest();
        if (down) {
            return status(503, error("ENGINE_DOWN", "down"));
        }
        String path = request.getUrl();
        if ("/games".equals(path) && request.getMethod().equals(RequestMethod.POST)) {
            return create(request);
        }
        Matcher matcher = GAME_PATH.matcher(path);
        if (!matcher.matches()) {
            return status(404, error("NOT_FOUND", path));
        }
        String gameId = matcher.group(1);
        Game game = games.get(gameId);
        if (game == null) {
            return status(404, error("GAME_NOT_FOUND", gameId));
        }
        return matcher.group(2) == null ? status(200, state(game)) : move(game, request);
    }

    private ResponseDefinition create(Request request) {
        String gameId = read(request).get("gameId").asText();
        if (games.containsKey(gameId)) {
            return status(409, error("GAME_ALREADY_EXISTS", gameId));
        }
        Game game = new Game(gameId);
        games.put(gameId, game);
        if (failCreateAfterApply) {
            failCreateAfterApply = false;
            return status(500, error("BOOM", "after apply"));
        }
        return status(201, state(game));
    }

    private ResponseDefinition move(Game game, Request request) {
        JsonNode body = read(request);
        char player = body.get("player").asText().charAt(0);
        int position = body.get("position").asInt();
        awaitHoldIfConfigured();
        MoveFault fault;
        synchronized (this) {
            fault = moveFaults.poll();
        }
        if (fault == MoveFault.RESET_BEFORE_APPLY) {
            return fault(Fault.CONNECTION_RESET_BY_PEER);
        }
        if (fault == MoveFault.WRONG_TURN) {
            return status(409, error("WRONG_TURN", "injected"));
        }
        String rejection = game.apply(player, position);
        if (rejection != null) {
            return status(409, error(rejection, rejection));
        }
        movesApplied.incrementAndGet();
        if (fault == MoveFault.RESET_AFTER_APPLY || (failWhenGameEnds && game.finished())) {
            return fault(Fault.CONNECTION_RESET_BY_PEER);
        }
        if (fault == MoveFault.DELAY_AFTER_APPLY) {
            return new ResponseDefinitionBuilder().withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody(state(game)).withFixedDelay(1500).build();
        }
        return status(200, state(game));
    }

    private void awaitHoldIfConfigured() {
        CountDownLatch gate = holdGate;
        if (gate != null && movesApplied.get() + 1 == holdBeforeMove) {
            holdReached.countDown();
            try {
                gate.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private JsonNode read(Request request) {
        try {
            return json.readTree(request.getBodyAsString());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String state(Game game) {
        ObjectNode node = json.createObjectNode();
        node.put("gameId", game.id);
        ArrayNode board = node.putArray("board");
        for (char cell : game.cells) {
            if (cell == '.') {
                board.addNull();
            } else {
                board.add(String.valueOf(cell));
            }
        }
        if (game.finished()) {
            node.putNull("nextPlayer");
        } else {
            node.put("nextPlayer", String.valueOf(game.next));
        }
        node.put("status", game.status);
        if (game.winningLine != null) {
            ArrayNode line = node.putArray("winningLine");
            for (int index : game.winningLine) {
                line.add(index);
            }
        } else {
            node.putNull("winningLine");
        }
        return node.toString();
    }

    private String error(String code, String message) {
        return json.createObjectNode().put("code", code).put("message", message).toString();
    }

    private static ResponseDefinition status(int status, String body) {
        return new ResponseDefinitionBuilder().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(body).build();
    }

    private static ResponseDefinition fault(Fault fault) {
        return new ResponseDefinitionBuilder().withFault(fault).build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private static final class Game {
        private final String id;
        private final char[] cells = ".........".toCharArray();
        private char next = 'X';
        private String status = "IN_PROGRESS";
        private int[] winningLine;

        private Game(String id) {
            this.id = id;
        }

        private synchronized String apply(char player, int position) {
            if (finished()) {
                return "GAME_FINISHED";
            }
            if (player != next) {
                return "WRONG_TURN";
            }
            if (cells[position] != '.') {
                return "CELL_OCCUPIED";
            }
            cells[position] = player;
            for (int[] line : LINES) {
                if (cells[line[0]] == player && cells[line[1]] == player && cells[line[2]] == player) {
                    winningLine = line;
                    status = player == 'X' ? "X_WON" : "O_WON";
                    return null;
                }
            }
            status = new String(cells).indexOf('.') < 0 ? "DRAW" : "IN_PROGRESS";
            next = player == 'X' ? 'O' : 'X';
            return null;
        }

        private boolean finished() {
            return !"IN_PROGRESS".equals(status);
        }

        private String board() {
            return new String(cells);
        }
    }
}
