package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

// 修正导入语句，确保引用正确的内部接口和类
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;

public final class MyModelFactory implements ScotlandYard.Factory<Model> {

	@Nonnull
	@Override
	public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new Model() {
			private GameState state = new MyGameStateFactory().build(setup, mrX, detectives);
			private final Set<Observer> observers = new CopyOnWriteArraySet<>();

			@Override
			public void chooseMove(@Nonnull Move move) {
				GameState newState = state.advance(move);
				state = newState;

				if (!state.getWinner().isEmpty()) {
					notifyObservers(Event.GAME_OVER);
				} else {
					notifyObservers(Event.MOVE_MADE);
				}
			}

			private void notifyObservers(Event event) {
				if (!state.getWinner().isEmpty() && event == Event.MOVE_MADE) {
					// 如果游戏已经结束，且事件是 MOVE_MADE，则改为发送 GAME_OVER 事件
					event = Event.GAME_OVER;
				}
				for (Observer observer : observers) {
					// 显式将 state 转换为 Board 类型（因为 GameState 是 Board 的子接口）
					observer.onModelChanged((Board) state, event);
				}
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observer == null) throw new NullPointerException("Observer cannot be null");
				if (!observers.add(observer))
					throw new IllegalArgumentException("Observer already registered");
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer == null) throw new NullPointerException("Observer cannot be null");
				if (!observers.remove(observer))
					throw new IllegalArgumentException("Observer not registered");
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observers);
			}

			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return state; // GameState 继承 Board，直接返回
			}
		};
	}
}