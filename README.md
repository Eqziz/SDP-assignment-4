# Flight Reservation System: Facade Over a Three-Stage Chain with Durable Persistence

Variant 8: **Flight Reservation**
- **Subsystem A (Intake / Reservation)**: Seat Map, Fare Calculator, Seat Hold & Availability Gateway
- **Subsystem B (Processing / Execution)**: E-Ticket Issuer, Baggage Rules Engine, Ticket Issuance Gateway
- **Subsystem C (Finalization / Confirmation)**: Loyalty Points Calculator, Meal Preference Policy, Loyalty Ledger Gateway

---

## 1. Architectural Overview

The system implements a coordinated three-stage transactional workflow orchestrated behind the **Facade Pattern**. From the client's perspective, booking a flight is a single logical operation (`bookFlight`). Behind the scenes, three independent subsystems collaborate in sequence:

1. **Step A (Intake / Reservation)**: Validates seat configuration, computes class and location fare, and reserves a seat hold (`HELD`).
2. **Step B (Processing / Execution)**: Validates baggage allowance, computes excess baggage fees, generates a unique IATA e-ticket serial number, and issues the ticket (`ISSUED`).
3. **Step C (Finalization / Confirmation)**: Validates passenger loyalty eligibility and catering policy, calculates loyalty points, and confirms the overall booking (`ACCRUED` -> `SETTLED`).

Each subsystem persists its state directly to embedded disk storage (JSON files in `data/`). The chain execution progress is journaled to disk at each step (`booking_journal.json`), enabling state reconstruction, crash reconciliation, and two-level compensation rollbacks.

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|                       FlightReservationApp (Console CLI)                          |
+-----------------------------------------------------------------------------------+
                   |                                           |
                   v                                           v
+-------------------------------------+     +-------------------------------------+
|        FlightBookingFacade          |     |       FlightOperationsFacade        |
|  - Drives Step A -> Step B -> Step C|     |  - Inspects cross-subsystem state   |
|  - Catches failures & unwinds chain |     |  - Reconciles dangling journal on   |
|  - Manages compensation depths 1 & 2|     |    restart; manual administrative   |
+-------------------------------------+     |    rollback & data store resets     |
                   |                        +-------------------------------------+
                   +-------------------+---------------+
                                       |
                   +-------------------+-------------------+
                   |                   |                   |
                   v                   v                   v
          +-----------------+ +-----------------+ +-----------------+
          |   Subsystem A   | |   Subsystem B   | |   Subsystem C   |
          |Seat Reservation | |Ticket & Baggage | |Loyalty & Confirm|
          +-----------------+ +-----------------+ +-----------------+
          |*SeatReservation | |*TicketIssuance  | |*LoyaltyAccount  |
          |  (HELD/CONFIRMED| |  (ISSUED/       | |  (ACCRUED/      |
          |   /RELEASED)    | |   COMPLETED/    | |   SETTLED/      |
          |*FareCalculator  | |   VOIDED)       | |   REVOKED)      |
          |*SeatMapValidator| |*BaggageRules    | |*LoyaltyPoints   |
          |*SeatReservation | |*TicketGenerator | |*MealPreference  |
          | Repository (disk| |*TicketIssuance  | |*LoyaltyRecord   |
          +-----------------+ | Repository (disk| | Repository (disk|
                              +-----------------+ +-----------------+
```

---

## 2. Independent Subsystems

Each subsystem is an autonomous domain module containing 4-5 classes, enforces legal state transitions internally on its entity, and talks to its own embedded disk repository. **Zero subsystems import or reference each other.**

### Subsystem A: Intake / Reservation Hold
- **`SeatReservation`**: Domain entity. Enforces valid state transitions:
  - `HELD` -> `CONFIRMED`
  - `HELD` -> `RELEASED`
  - `CONFIRMED` -> `RELEASED`
  - Illegal transitions (e.g. `RELEASED` -> `CONFIRMED`) throw `IllegalStateException`.
- **`SeatReservationStatus`**: State enumeration (`HELD`, `CONFIRMED`, `RELEASED`).
- **`FareCalculator`**: Pure business logic computing base cabin rates (`ECONOMY` = $200, `BUSINESS` = $440, `FIRST` = $750), location surcharges (exit rows +$35, window/aisle +$15), and airport taxes ($25).
- **`SeatMapValidator`**: Pure business logic validating layout format (`^[1-35][A-F]$`) and preventing double-booking against existing active holds.
- **`SeatReservationRepository`**: Persistence gateway reading and writing directly to `data/seat_reservations.json`.

### Subsystem B: Processing / Execution (Ticket & Baggage)
- **`TicketIssuance`**: Domain entity. Enforces valid state transitions:
  - `ISSUED` -> `COMPLETED`
  - `ISSUED` -> `VOIDED`
  - `COMPLETED` -> `VOIDED`
  - Illegal transitions throw `IllegalStateException`.
- **`TicketStatus`**: State enumeration (`ISSUED`, `COMPLETED`, `VOIDED`).
- **`BaggageRulesEngine`**: Pure business logic enforcing airline baggage policy:
  - Allowance: 20.0 kg included free.
  - Excess fee: $15.00 per kg above 20.0 kg.
  - Maximum limit: 32.0 kg per passenger bag. If weight > 32.0 kg, throws `ExcessBaggageException` (triggering Step B compensation).
- **`TicketNumberGenerator`**: Pure business logic formatting IATA standard tickets (`ETKT-2026-XXXXXX`).
- **`TicketIssuanceRepository`**: Persistence gateway reading and writing directly to `data/ticket_issuances.json`.

### Subsystem C: Finalization / Confirmation (Loyalty & Catering)
- **`LoyaltyAccountRecord`**: Domain entity. Enforces valid state transitions:
  - `ACCRUED` -> `SETTLED`
  - `ACCRUED` -> `REVOKED`
  - `SETTLED` -> `REVOKED`
  - Illegal transitions throw `IllegalStateException`.
- **`LoyaltyStatus`**: State enumeration (`ACCRUED`, `SETTLED`, `REVOKED`).
- **`LoyaltyPointsCalculator`**: Pure business logic computing miles/points based on fare paid and membership tier multiplier (`STANDARD` = 1.0x, `SILVER` = 1.25x, `GOLD` = 1.5x, `PLATINUM` = 2.0x).
- **`MealPreferenceValidator`**: Pure business logic validating catering restrictions (`STANDARD`, `VEGAN`, `HALAL`, `KOSHER`, `GLUTEN_FREE`). If member account is suspended (`SUSPENDED_USER` / `RESTRICTED_...`) or meal is rejected, throws `LoyaltyProcessingException` (triggering Step C compensation).
- **`LoyaltyRecordRepository`**: Persistence gateway reading and writing directly to `data/loyalty_records.json`.

---

## 3. The Facades

The system provides two distinct facades:

### 1. `FlightBookingFacade` (Primary Client Workflow)
- **`bookFlight(BookingRequest request): BookingResult`**
  - Performs pre-commit validation.
  - Initializes transaction journal record on disk.
  - Executes Step A (hold seat on disk).
  - Executes Step B (validates baggage & issues ticket on disk).
  - Executes Step C (validates loyalty/meal, settles all three subsystem records on disk).
  - If Step B or C fails, detects it and triggers the corresponding compensation depth.

### 2. `FlightOperationsFacade` (Administrative & Recovery Workflow)
- **`inspectBooking(String bookingReference): ReservationInspection`**: Correlates persisted records across all 3 subsystems and journal to evaluate atomic consistency.
- **`reconcilePendingTransactions(): int`**: Runs upon application restart; scans `booking_journal.json` for uncompleted chains and executes rollback recovery from disk.
- **`manualRollback(String bookingReference): boolean`**: Administrative compensation for existing bookings.
- **`listAllTransactions(): List<BookingTransaction>`**: Returns persisted audit history.
- **`resetAllStores(): void`**: Cleans all disk stores before fresh runs.

---

## 4. Cross-Subsystem Consistency & Compensation Depths

Real chains break in the middle. The facade detects failures using what was actually written to disk, unwinding the chain in reverse order:

### Compensation Depth 1: Step B Fails After Step A Committed
1. **Scenario**: Passenger holds seat `14B` on flight `AA102` (persisted to disk as `HELD`).
2. **Failure Point**: Passenger requests 40.0 kg baggage weight, exceeding the 32.0 kg hard safety limit.
3. **Detection**: `BaggageRulesEngine` throws `ExcessBaggageException`.
4. **Unwind Action**:
   - Facade updates journal to `COMPENSATING_AFTER_B_FAILURE`.
   - Facade compensates Subsystem A alone: calls `seatReservation.transitionTo(RELEASED)` and persists to disk.
   - Subsystem B ticket was never committed.
   - Journal updated to `COMPENSATED_A_ONLY`.
5. **Disk State**: Subsystem A = `RELEASED`, Subsystem B = absent, Subsystem C = absent.

### Compensation Depth 2: Step C Fails After Step A and Step B Committed
1. **Scenario**: Passenger holds seat `16C` (persisted as `HELD`) and ticket `ETKT-2026-100001` is issued (persisted as `ISSUED`).
2. **Failure Point**: Passenger member ID is flagged as `SUSPENDED_USER_88` or catering policy rejects meal.
3. **Detection**: `MealPreferenceValidator` throws `LoyaltyProcessingException`.
4. **Unwind Action (Two-Step Rollback)**:
   - Facade updates journal to `COMPENSATING_AFTER_C_FAILURE`.
   - **Step 1 of rollback**: Facade compensates Subsystem B: calls `ticketIssuance.transitionTo(VOIDED)` and updates disk.
   - **Step 2 of rollback**: Facade compensates Subsystem A: calls `seatReservation.transitionTo(RELEASED)` and updates disk.
   - Journal updated to `COMPENSATED_FULL`.
5. **Disk State**: Subsystem A = `RELEASED`, Subsystem B = `VOIDED`, Subsystem C = absent.

### Pre-Commit Failure at Step A
- If seat format is invalid (e.g., `99Z`) or seat is already occupied, `SeatMapValidator` throws `SeatUnavailableException` before any records are committed to disk.

---

## 5. Principle of Least Knowledge (Law of Demeter)

### Before: Client Talking Directly to Three Subsystems
Without the facade, the client would need direct coupling to at least 10 classes across 3 independent subsystems, orchestrate sequencing manually, track state across multiple repositories, and handle rollbacks:

```java
// BEFORE (Without Facade: Massive coupling, violates Least Knowledge)
SeatReservationRepository seatRepo = new SeatReservationRepository(seatPath);
FareCalculator fareCalc = new FareCalculator();
SeatMapValidator validator = new SeatMapValidator();

validator.validateSeatFormat("12A");
validator.ensureSeatAvailable("AA101", "12A", seatRepo);
double fare = fareCalc.calculateFare("ECONOMY", "12A");
SeatReservation seat = SeatReservation.create("RES-1", "AA101", "PAX-1", "12A", "ECONOMY", fare);
seatRepo.save(seat);

TicketIssuanceRepository ticketRepo = new TicketIssuanceRepository(ticketPath);
BaggageRulesEngine baggageEngine = new BaggageRulesEngine();
TicketNumberGenerator generator = new TicketNumberGenerator();

TicketIssuance ticket = null;
try {
    double baggageFee = baggageEngine.validateAndCalculateFee(40.0); // FAILS HERE!
    String tktNum = generator.generateTicketNumber();
    ticket = TicketIssuance.issue(tktNum, "BKG-1", "Alice", "AA101", 40.0, baggageFee);
    ticketRepo.save(ticket);
} catch (ExcessBaggageException ex) {
    // Client itself must know Subsystem A internals to unwind:
    seat.transitionTo(SeatReservationStatus.RELEASED);
    seatRepo.update(seat);
    throw ex;
}

LoyaltyRecordRepository loyaltyRepo = new LoyaltyRecordRepository(loyaltyPath);
MealPreferenceValidator mealVal = new MealPreferenceValidator();
LoyaltyPointsCalculator pointsCalc = new LoyaltyPointsCalculator();
try {
    String meal = mealVal.validate("VEGAN", "SUSPENDED_USER"); // FAILS HERE!
    int pts = pointsCalc.calculatePoints(fare + ticket.getExcessBaggageFee(), "GOLD");
    LoyaltyAccountRecord loyalty = LoyaltyAccountRecord.accrue("CONF-1", "BKG-1", "SUSPENDED_USER", "GOLD", pts, meal, fare);
    loyaltyRepo.save(loyalty);
} catch (LoyaltyProcessingException ex) {
    // Client itself must know how to rollback Subsystem B, then Subsystem A:
    ticket.transitionTo(TicketStatus.VOIDED);
    ticketRepo.update(ticket);
    seat.transitionTo(SeatReservationStatus.RELEASED);
    seatRepo.update(seat);
    throw ex;
}
```

### After: Client Interacting Through the Facade
With the facade, the client interacts with only one immediate friend: `FlightBookingFacade`. It passes a `BookingRequest` and receives a `BookingResult`.

```java
// AFTER (With Facade: Principle of Least Knowledge Satisfied)
FlightBookingFacade facade = getBookingFacade();

BookingRequest request = BookingRequest.builder()
        .flightNumber("AA101")
        .passengerId("PAX-1")
        .passengerName("Alice")
        .seatNumber("12A")
        .seatClass("ECONOMY")
        .baggageWeightKg(18.0)
        .memberId("MEM-101")
        .membershipTier("GOLD")
        .mealPreference("VEGAN")
        .build();

BookingResult result = facade.bookFlight(request);
if (result.isSuccess()) {
    System.out.println("Confirmed Booking: " + result.getBookingReference());
} else {
    System.out.println("Booking Compensated: " + result.getFailureReason());
}
```

---

## 6. Reflection: Facade vs. Adapter

In object-oriented design, **Facade** and **Adapter** are structural patterns that address fundamentally different architectural intents:

- **Adapter's intent is interface translation**: It wraps an existing, incompatible class to make it conform to an interface expected by a specific client. Adapter enables polymorphism where two different objects must be used interchangeably behind a shared contract without altering existing client code.
- **Facade's intent is subsystem simplification and encapsulation**: It introduces a higher-level, unified interface over a collection of distinct, heterogeneous subsystems. Facade does not match an existing interface; it designs a *new*, streamlined abstraction that coordinates sequencing, transaction boundaries, and error compensation across unrelated components.

In our **Flight Reservation System**, Subsystem A (Seat Reservation), Subsystem B (Ticket Issuance), and Subsystem C (Loyalty Program) are not alternative implementations of a single service. They represent separate business capabilities that must execute in a strict logical chain. The client does not need to adapt a `SeatReservation` into a `TicketIssuance`. Instead, the client requires a single coordinated entry point that drives the multi-step lifecycle, writes durable state to disk at each step, and orchestrates rollback compensation when downstream steps fail.

Attempting to apply an Adapter here would be conceptually erroneous: there is no target interface to adapt to. The problem requires coordination, transaction boundary management, and cross-subsystem orchestration—the exact design purpose of the **Facade Pattern**.

---

## 7. Build, Test, and Live Demo Execution

### Prerequisites
- JDK 21 (or JDK 17+)
- Apache Maven 3.8+

### Single Command to Run Tests & JaCoCo Coverage
```bash
mvn clean test
```
- Total Tests: **36 passed (0 failures, 0 errors)**
- Line Coverage: **>90% across all subsystems and facades** (measured via JaCoCo).
- Coverage report available at: `target/site/jacoco/index.html`.

### Single Command to Launch the Interactive Demo CLI
```bash
mvn compile exec:java
```

### Resetting Storage to a Clean State
- **From within the CLI menu**: Select **Option 10 (Reset Storage)**.
- **From the command line**: Delete the contents of the `data/` folder, or let the CLI recreate clean empty files automatically.
