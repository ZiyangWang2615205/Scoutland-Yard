# Scotland Yard

A Java implementation of the Scotland Yard board game model and AI player.

This repository contains my coursework implementation for the Scotland Yard project. The project focuses on object-oriented design, immutable game state modelling, move generation, game progression, observer-style model updates, and AI decision-making.

> This public repository only contains permitted visible code; restricted coursework implementation details are not included.

## Project Overview

The coursework is split into two main components:

```text
.
├── cw-model/   # Core Scotland Yard game model
└── cw-ai/      # AI extension for automated play
```

## cw-model

The `cw-model` project implements the core game logic for Scotland Yard.

The model is responsible for:

- representing the game setup, players, tickets, locations and travel log
- generating all legal moves for Mr X and the detectives
- handling single moves, secret moves and double moves
- advancing the game from one immutable state to the next
- updating player locations and ticket counts
- determining when Mr X or the detectives have won
- supporting the provided UI and test suite

The main implementation work is centred around:

```text
src/main/java/uk/ac/bris/cs/scotlandyard/model/
```

Important implementation areas include:

- `MyGameStateFactory`
- `MyModelFactory`
- game-state validation
- available-move generation
- `advance(Move move)`
- winner detection
- observer/model behaviour

## cw-ai

The `cw-ai` project extends the game with an automated player.

The AI implementation is based around the `Ai` interface and its `pickMove` method. The initial skeleton selects random moves, but the intended implementation improves this with a stronger decision strategy.

Possible AI features include:

- Mr X move scoring
- distance-based evaluation
- look-ahead search
- MiniMax-style game-tree search
- alpha-beta pruning
- optional detective AI
- dynamic depth or performance improvements

The main AI class is located around:

```text
src/main/java/uk/ac/bris/cs/scotlandyard/ui/ai/
```

## Technologies Used

- Java 17
- Maven
- JUnit test suite
- Guava immutable collections and graph utilities
- IntelliJ IDEA / command-line Maven workflow

## Requirements

Use Java 17 unless otherwise instructed by the coursework skeleton.

Check your Java version with:

```bash
java --version
```

The project uses Maven Wrapper, so Maven does not need to be installed globally.

On Unix/macOS, make sure the wrapper is executable:

```bash
chmod +x mvnw
```

## Building the Project

From the root of either `cw-model` or `cw-ai`, run:

```bash
./mvnw clean compile
```

On Windows PowerShell:

```powershell
mvnw clean compile
```

## Running Tests

For `cw-model`, run all tests with:

```bash
./mvnw clean test
```

To run a single test class:

```bash
./mvnw -Dtest=GameStateCreationTest test
```

To run a specific test method:

```bash
./mvnw -Dtest=GameStateCreationTest#testNullMrXShouldThrow* test
```

## Running the GUI

For `cw-model`:

```bash
./mvnw clean compile exec:java
```

For `cw-ai`:

```bash
./mvnw clean compile exec:java
```

The GUI can be used to play the game locally and test the model or AI behaviour.

## Implementation Notes

### Game Model

The model implementation follows an immutable-state approach. Instead of mutating an existing game state, applying a move returns a new updated game state.

Key logic includes:

- validating constructor inputs
- checking that detectives have unique locations
- ensuring player pieces are valid and non-duplicated
- generating legal single moves
- generating legal double moves for Mr X
- supporting secret tickets
- preventing Mr X from moving onto detective-occupied locations
- transferring detective tickets to Mr X
- discarding Mr X tickets after use
- updating Mr X’s travel log with revealed or hidden moves
- switching turns between Mr X and detectives
- skipping detectives that cannot move
- detecting game-over conditions

### Winner Conditions

Detectives win when:

- a detective lands on Mr X’s location
- Mr X has no valid move available

Mr X wins when:

- the travel log is full and he has not been caught
- the detectives can no longer move

### AI Strategy

The AI can be improved progressively:

1. Start with random legal move selection.
2. Add a scoring function for Mr X.
3. Prefer moves that increase distance from detectives.
4. Add look-ahead search.
5. Extend toward MiniMax or alpha-beta pruning.
6. Optimise for speed, depth and better decision quality.

## Useful Maven Commands

Compile:

```bash
./mvnw clean compile
```

Run tests:

```bash
./mvnw clean test
```

Generate Javadocs:

```bash
./mvnw clean compile javadoc:javadoc
```

Run application:

```bash
./mvnw clean compile exec:java
```

Clean compiled files before packaging:

```bash
./mvnw clean
```
