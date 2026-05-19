package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

public interface Board {

	interface TicketBoard {
		int getCount(@Nonnull Ticket ticket);
	}

	@Nonnull GameSetup getSetup();
	@Nonnull ImmutableSet<Piece> getPlayers();
	@Nonnull Optional<Integer> getDetectiveLocation(Detective detective);
	@Nonnull Optional<TicketBoard> getPlayerTickets(Piece piece);
	@Nonnull ImmutableList<LogEntry> getMrXTravelLog();
	@Nonnull ImmutableSet<Piece> getWinner();
	@Nonnull ImmutableSet<Move> getAvailableMoves();

	// GameState 明确继承 Board
	interface GameState extends Board {
		@Nonnull GameState advance(Move move);
	}
}