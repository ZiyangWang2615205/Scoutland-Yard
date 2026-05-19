package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {
		private final GameSetup setup;
		private final ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;
		private final Player mrX;
		private final List<Player> detectives;
		private final ImmutableSet<Move> moves;
		private final ImmutableSet<Piece> winner;
		private final Map<Piece, Player> playerMap;
//创建mygamestate class去实现gamestate接口

		private MyGameState(//构建inner class的mygamestate的constructor
				final GameSetup setup,
				final ImmutableSet<Piece> remainingParam,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives,
				final ImmutableSet<Move> movesParam
		) {
			validateSetup(setup, detectives);//用创建的validate steup函数通过creation test

			this.setup = setup;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;//初始化

			// set the map of player with its piece
			this.playerMap = new HashMap<>();
			playerMap.put(mrX.piece(), mrX);
			detectives.forEach(d -> playerMap.put(d.piece(), d));//创建一个hashmap将所有人物丢进去，并且把他们的piece和身份相互映射

			// To find winner, as long as find it, delete the remaining and moves
			ImmutableSet<Piece> tempWinner = calculateWinners(movesParam);//用calculatewinner函数寻找是否产生winner
			if (!tempWinner.isEmpty()) {
				this.remaining = ImmutableSet.of();
				this.moves = ImmutableSet.of();
			} else {
				this.remaining = remainingParam;
				this.moves = movesParam;
			}
			this.winner = tempWinner;
		}

		// for set up if there has anything against init throw the mistake message
		private void validateSetup(GameSetup setup, List<Player> detectives) {
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if (detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			detectives.forEach(d -> {
				if (d.has(Ticket.DOUBLE) || d.has(Ticket.SECRET))
					throw new IllegalArgumentException("Detective has illegal tickets: " + d.piece());
			});
			Set<Integer> locations = detectives.stream().map(Player::location).collect(Collectors.toSet());//在steam中对所有player调用location（）方法，获取location并放入set中
			if (locations.size() != detectives.size()){
				throw new IllegalArgumentException("Duplicate detective locations");//set中不允许有重复元素，且每一个侦探对应唯一位置所以一旦locations set的大小和侦探的数量没对上就会报错;
			}
		}


		private ImmutableSet<Piece> calculateWinners(ImmutableSet<Move> currentMoves) {//这是我们计算胜者的函数
			//this is the set what we return for winner
			ImmutableSet.Builder<Piece> winnerBuilder = ImmutableSet.builder();
			//give a boolean to judge whether we catch mrx or not, as long as we catch it , mrx lose
			boolean mrXCatch = false;
			for(Player player : detectives) {
				if(player.location()==mrX.location()){
					mrXCatch = true;
				}
			}
			if(mrXCatch){
				for(Player player : detectives) {
					winnerBuilder.add(player.piece());
				}
			} else if (log.size() >= setup.moves.size()) {// if the round reach end and mrx did not be captured, mrx win
				winnerBuilder.add(MrX.MRX);
			} else if (currentMoves.isEmpty()) {//if mrx cornered and no way can escape, mrx lose
				for (Player player : detectives) {
					winnerBuilder.add(player.piece());
				}
			} else {//if all detective stuck, mrx did not be captured, mrx win
				boolean detectivesStuck = detectives.stream()
						.allMatch(d -> calculateAvailableMoves(setup, detectives, d).isEmpty());
				if (detectivesStuck) {
					winnerBuilder.add(MrX.MRX);
				}

			}
			return winnerBuilder.build();
		}

		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			return remaining;
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			return detectives.stream()
					.filter(d -> d.piece().equals(detective))//go through player and choose the detective by its piece
					.findFirst()
					.map(Player::location);//after choose detective them map to its location
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			return Optional.ofNullable(playerMap.get(piece))//use map to find player,and use ofnullable() to avoid null pointer
					.map(p -> ticket -> p.tickets().getOrDefault(ticket, 0));
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		@Override
		public GameState advance(Move move) {
			// if game over return
			if (!winner.isEmpty()) return this;
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			Piece mover = move.commencedBy();
			Player currentPlayer = playerMap.get(mover);
			Player updatedPlayer = currentPlayer;
			List<Ticket> usedTickets = new ArrayList<>();
			int destination = -1;

			// deal with single move and double move
			if (move instanceof SingleMove) {
				SingleMove single = (SingleMove) move;
				updatedPlayer = currentPlayer.use(single.ticket).at(single.destination);
				usedTickets.add(single.ticket);
				destination = single.destination;
			} else if (move instanceof DoubleMove) {
				DoubleMove dm = (DoubleMove) move;
				updatedPlayer = currentPlayer.use(Ticket.DOUBLE)
						.use(dm.ticket1)
						.use(dm.ticket2)
						.at(dm.destination2);
				usedTickets.add(dm.ticket1);
				usedTickets.add(dm.ticket2);
				destination = dm.destination2;
			}

			// update new status
			Player newMrX = mrX;
			List<Player> newDetectives = new ArrayList<>(detectives);
			if (mover.isMrX()) {
				newMrX = updatedPlayer;
			} else {
				for (int i = 0; i < newDetectives.size(); i++) {
					if (newDetectives.get(i).piece().equals(mover)) {
						newDetectives.set(i, updatedPlayer);
						break;
					}
				}
				newMrX = mrX.give(usedTickets);
			}

			// update new log of mrx
			ImmutableList<LogEntry> newLog = log;
			if (mover.isMrX()) {
				ImmutableList.Builder<LogEntry> logBuilder = ImmutableList.builder();
				logBuilder.addAll(log);
				if (move instanceof SingleMove) {
					SingleMove single = (SingleMove) move;
					boolean reveal = setup.moves.get(log.size());
					logBuilder.add(reveal ? LogEntry.reveal(single.ticket, single.destination)
							: LogEntry.hidden(single.ticket));
				} else if (move instanceof DoubleMove) {
					DoubleMove dm = (DoubleMove) move;
					boolean reveal1 = setup.moves.get(log.size());
					logBuilder.add(reveal1 ? LogEntry.reveal(dm.ticket1, dm.destination1)
							: LogEntry.hidden(dm.ticket1));
					boolean reveal2 = setup.moves.get(log.size() + 1);
					logBuilder.add(reveal2 ? LogEntry.reveal(dm.ticket2, dm.destination2)
							: LogEntry.hidden(dm.ticket2));
				}
				newLog = logBuilder.build();
			}

			// update player,if have moved be removed from remaining
			// if all detective moved，next turn is Mr.X
			ImmutableSet<Piece> newRemaining;
			if (mover.isMrX()) {

				// when detective's turn comes,check if detective can move
				Set<Piece> detectivesWithMoves = new HashSet<>();
				for (Player detective : newDetectives) {
					ImmutableSet<Move> movesForDetective = calculateAvailableMoves(setup, newDetectives, detective);
					if (!movesForDetective.isEmpty()) {
						detectivesWithMoves.add(detective.piece());
					}
				}
				if (!detectivesWithMoves.isEmpty()) {
					newRemaining = ImmutableSet.copyOf(detectivesWithMoves);
				} else {
					newRemaining = ImmutableSet.of(mrX.piece());
				}
			} else {
				// after moving, remove player from remaining
				newRemaining = remaining.stream()
						.filter(p -> !p.equals(mover))
						.collect(ImmutableSet.toImmutableSet());
				if (newRemaining.isEmpty()) {
					newRemaining = ImmutableSet.of(mrX.piece());
				}
			}

			// calculate the new move
			ImmutableSet<Move> newMoves;
			if (newRemaining.isEmpty()) {
				newMoves = ImmutableSet.of();
			} else {
				ImmutableSet.Builder<Move> movesBuilder = ImmutableSet.builder();
				for (Piece piece : newRemaining) {
					Player player = piece.isMrX() ?
							newMrX : newDetectives.stream()
							.filter(d -> d.piece().equals(piece))
							.findFirst().orElseThrow();
					movesBuilder.addAll(calculateAvailableMoves(setup, newDetectives, player));
				}
				newMoves = movesBuilder.build();
			}

			return new MyGameState(setup, newRemaining, newLog, newMrX, newDetectives, newMoves);
		}
	}

	// judge whether the place occupied by detective or not
	private static boolean isOccupied(List<Player> detectives, int location) {
		return detectives.stream().anyMatch(d -> d.location() == location);
	}

	private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		Set<SingleMove> moves = new HashSet<>();
		for (int dest : setup.graph.adjacentNodes(source)) {
			if (isOccupied(detectives, dest)) continue;
			for (Transport t : setup.graph.edgeValueOrDefault(source, dest, ImmutableSet.of())) {
				if (player.has(t.requiredTicket())) {
					moves.add(new SingleMove(player.piece(), source, t.requiredTicket(), dest));
				}
			}
			if (player.has(Ticket.SECRET)) {
				moves.add(new SingleMove(player.piece(), source, Ticket.SECRET, dest));
			}
		}
		return moves;
	}

	private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		Set<DoubleMove> moves = new HashSet<>();
		if (player.has(Ticket.DOUBLE) && setup.moves.size() > 1) {
			Set<SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
			for (SingleMove first : firstMoves) {
				Player afterFirst = player.use(first.ticket);
				Set<SingleMove> secondMoves = makeSingleMoves(setup, detectives, afterFirst, first.destination);
				for (SingleMove second : secondMoves) {
					if (afterFirst.has(second.ticket)) {
						moves.add(new DoubleMove(player.piece(), source,
								first.ticket, first.destination,
								second.ticket, second.destination));
					}
				}
			}
		}
		return moves;
	}

	// calculate the num of moves for player
	private ImmutableSet<Move> calculateAvailableMoves(GameSetup setup, List<Player> detectives, Player player) {
		ImmutableSet.Builder<Move> builder = ImmutableSet.builder();
		builder.addAll(makeSingleMoves(setup, detectives, player, player.location()));
		if (player.isMrX()) {
			builder.addAll(makeDoubleMoves(setup, detectives, player, player.location()));
		}
		return builder.build();
	}

	@Nonnull
	@Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		ImmutableSet<Piece> remaining = ImmutableSet.<Piece>builder()
				.add(MrX.MRX)
				.addAll(detectives.stream().map(Player::piece).iterator())
				.build();
		ImmutableSet<Move> moves = calculateAvailableMoves(setup, detectives, mrX);
		return new MyGameState(setup, remaining, ImmutableList.of(), mrX, detectives, moves);
	}
}
