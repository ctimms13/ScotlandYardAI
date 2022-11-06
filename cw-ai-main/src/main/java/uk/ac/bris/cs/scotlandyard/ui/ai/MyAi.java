package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	// genarates a score for a given board
	// maximising player mrX
	private int genScore (Board board){
		int score = board.getAvailableMoves().size();
		int funky = new Random().nextInt(5);
		int detCheck = checkDetectives(board);
		int subtract = detCheck * 10;

		return score + funky - subtract;
	}

	//IF YOU DELETE THIS I WILL GO NUCLEAR ~ Charlotte 28/04/2021
	//Thanks ~ Charlotte 5/05/2021

	//Function to get the location of mrX based on the travel log
	private Optional<Integer> getLoc (Board board){
		ImmutableList<LogEntry> logs = board.getMrXTravelLog();
		LogEntry loggy =  logs.get(logs.size() -1); //get most recent log entry
		Optional<Integer> mrXLoc = loggy.location(); //get the location from that log entry as optional
		if (mrXLoc == null){
			throw new IllegalArgumentException("Murder me");
		}
		return mrXLoc;
	}

	//Check if the detectives are adjacent to where Mr X wants to move to
	private int checkDetectives (Board board){
		int count = 0;
		int checkLoc =  getLoc(board).orElse(-1); //if Mr X is null case
		GameSetup setup = board.getSetup();
		ImmutableSet<Piece> detectives = ImmutableSet.copyOf(board.getPlayers().stream()
										.filter(c -> c.isDetective()).collect(Collectors.toSet()));
		if(checkLoc != -1){
			for(int checkNode : setup.graph.adjacentNodes(checkLoc)){
				for (Piece detective : detectives){
					if (checkNode == board.getDetectiveLocation((Piece.Detective)detective).orElse(-1)){
						count += 1;
					}
				}
			}
		}
		return count;
	}

	//Apply the move to a given board using the advance method
	private Board makeMove (Board board, Move move){
		return ((Board.GameState) board).advance(move);
	}

	//Check if Mr X is in the remaining players
	private boolean isMrXRemaining(Board board){
		for (Move move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()) return true;
		}
		return false;
	}

	//Apply all the detective moves to the board
	private List<Board> applyAllDetectiveMoves (Board board){
		if (isMrXRemaining(board)) {
			return List.of(board);
		}

		List<Board> boards = new ArrayList<>();
		List<Board> done = new ArrayList<>();

		for (Move move : board.getAvailableMoves()){
			Board moveMadeBoard = makeMove(board, move);

			boards.addAll(applyAllDetectiveMoves(makeMove(board, move)));
			done.add(moveMadeBoard);

		}

		return boards;
	}

	// Min-Max algorithm for the AI, sort through the possible moves and score them
	private int minimax(Board board, int depth, boolean isMrX, int alpha, int beta){
		if (depth == 0 || !board.getWinner().isEmpty()){
			return genScore(board);
		}

		if (isMrX){
			int maxEval = (int)Double.NEGATIVE_INFINITY;

			for (Move move : board.getAvailableMoves()){
				Board moveMade = makeMove(board, move);

				int eval = minimax(moveMade, depth-1, false, alpha, beta);

				maxEval = Math.max(maxEval, eval);

				alpha = Math.max(alpha, eval);

				if (beta <= alpha) break;
			}

			return maxEval;

		} else {
			int minEval = (int)Double.POSITIVE_INFINITY;

			for (Board boardDetective : applyAllDetectiveMoves(board)){
				int eval = minimax(boardDetective, depth-1, true, alpha, beta);
				minEval = Math.min(minEval, eval);

				beta = Math.min(beta, eval);

				if (beta <= alpha) break;
			}

			return minEval;

		}
	}

	//wrapper function for min-max to work with Moves instead of just integers
	//Pass the possible board and move to the min-max function
	private Move applyMiniMax(Board board, int depth){
		int maxEval = (int)Double.NEGATIVE_INFINITY;
		int alpha = (int)Double.NEGATIVE_INFINITY;
		int beta = (int)Double.POSITIVE_INFINITY;

		List<Move> possibleMoves = new ArrayList<>();

		for (Move move : board.getAvailableMoves()){
			Board moveMade = makeMove(board, move);
			int eval = minimax(board, depth-1, false, alpha, beta);

			if (eval > maxEval){
				maxEval = eval;
				possibleMoves = new ArrayList<>();
				possibleMoves.add(move);

			} else if (eval == maxEval){
				possibleMoves.add(move);
			}
		}

		Random rand = new Random();
		return possibleMoves.get(rand.nextInt(possibleMoves.size()));
	}

	@Nonnull @Override public String name() { return "Mark"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		Move move = applyMiniMax(board, 3);

		return move;

	}


}
