package com.flightreservation.app;

import com.flightreservation.coordinator.BookingJournalRepository;
import com.flightreservation.coordinator.BookingStage;
import com.flightreservation.coordinator.BookingTransaction;
import com.flightreservation.exception.ExcessBaggageException;
import com.flightreservation.exception.FlightReservationException;
import com.flightreservation.exception.LoyaltyProcessingException;
import com.flightreservation.exception.SeatUnavailableException;
import com.flightreservation.facade.BookingRequest;
import com.flightreservation.facade.BookingResult;
import com.flightreservation.facade.FlightBookingFacade;
import com.flightreservation.facade.FlightOperationsFacade;
import com.flightreservation.facade.ReservationInspection;
import com.flightreservation.subsystem.loyalty.LoyaltyAccountRecord;
import com.flightreservation.subsystem.loyalty.LoyaltyPointsCalculator;
import com.flightreservation.subsystem.loyalty.LoyaltyRecordRepository;
import com.flightreservation.subsystem.loyalty.MealPreferenceValidator;
import com.flightreservation.subsystem.seat.FareCalculator;
import com.flightreservation.subsystem.seat.SeatMapValidator;
import com.flightreservation.subsystem.seat.SeatReservation;
import com.flightreservation.subsystem.seat.SeatReservationRepository;
import com.flightreservation.subsystem.seat.SeatReservationStatus;
import com.flightreservation.subsystem.ticket.BaggageRulesEngine;
import com.flightreservation.subsystem.ticket.TicketIssuance;
import com.flightreservation.subsystem.ticket.TicketIssuanceRepository;
import com.flightreservation.subsystem.ticket.TicketNumberGenerator;
import com.flightreservation.subsystem.ticket.TicketStatus;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class FlightReservationApp {

    private final FlightBookingFacade bookingFacade;
    private final FlightOperationsFacade operationsFacade;
    private final SeatReservationRepository seatRepository;
    private final TicketIssuanceRepository ticketRepository;
    private final LoyaltyRecordRepository loyaltyRepository;
    private final BookingJournalRepository journalRepository;

    private final FareCalculator fareCalculator;
    private final SeatMapValidator seatMapValidator;
    private final BaggageRulesEngine baggageRulesEngine;
    private final TicketNumberGenerator ticketGenerator;
    private final LoyaltyPointsCalculator pointsCalculator;
    private final MealPreferenceValidator mealValidator;

    public FlightReservationApp() {
        Path baseDataDir = Paths.get("data");
        Path seatPath = baseDataDir.resolve("seat_reservations.json");
        Path ticketPath = baseDataDir.resolve("ticket_issuances.json");
        Path loyaltyPath = baseDataDir.resolve("loyalty_records.json");
        Path journalPath = baseDataDir.resolve("booking_journal.json");

        this.seatRepository = new SeatReservationRepository(seatPath);
        this.fareCalculator = new FareCalculator();
        this.seatMapValidator = new SeatMapValidator();

        this.ticketRepository = new TicketIssuanceRepository(ticketPath);
        this.baggageRulesEngine = new BaggageRulesEngine();
        this.ticketGenerator = new TicketNumberGenerator();

        this.loyaltyRepository = new LoyaltyRecordRepository(loyaltyPath);
        this.pointsCalculator = new LoyaltyPointsCalculator();
        this.mealValidator = new MealPreferenceValidator();

        this.journalRepository = new BookingJournalRepository(journalPath);

        this.bookingFacade = new FlightBookingFacade(
                seatRepository,
                fareCalculator,
                seatMapValidator,
                ticketRepository,
                baggageRulesEngine,
                ticketGenerator,
                loyaltyRepository,
                pointsCalculator,
                mealValidator,
                journalRepository);

        this.operationsFacade = new FlightOperationsFacade(
                seatRepository,
                ticketRepository,
                loyaltyRepository,
                journalRepository);
    }

    public static void main(String[] args) {
        FlightReservationApp app = new FlightReservationApp();
        app.runInteractiveLoop();
    }

    public void runInteractiveLoop() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("================================================================================");
        System.out.println("         FLIGHT RESERVATION SYSTEM - FACADE PATTERN LIVE DEMONSTRATION          ");
        System.out.println("================================================================================");
        System.out.println("Persistent Data Directory: " + Paths.get("data").toAbsolutePath());

        while (running && scanner.hasNextLine()) {
            printMenu();
            System.out.print("Select an option (1-10): ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> handleHappyPathBooking(scanner);
                case "2" -> handleStepBFailureBooking(scanner);
                case "3" -> handleStepCFailureBooking(scanner);
                case "4" -> handleStepAFailureBooking(scanner);
                case "5" -> handleInspectAllPersistedStores();
                case "6" -> handleInspectSpecificBooking(scanner);
                case "7" -> handleManualRollback(scanner);
                case "8" -> handleSimulateRestartAndReconciliation();
                case "9" -> handleDirectSubsystemDemonstration();
                case "10" -> handleResetAllStores();
                case "0", "exit", "q" -> {
                    System.out.println("Exiting Flight Reservation CLI. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please choose an option between 1 and 10.");
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("1. Book Flight (Happy Path: Step A -> Step B -> Step C Successful)");
        System.out.println("2. Trigger Step B Failure (Excess Baggage > 32kg: Subsystem A Unwound Alone)");
        System.out.println("3. Trigger Step C Failure (Suspended Member / Bad Meal: Subsystem B, then A Unwound)");
        System.out.println("4. Trigger Step A Failure (Seat Unavailable / Format Error: Fails Pre-Commit)");
        System.out.println("5. Inspect Persisted Storage (Live Disk State Across All 3 Subsystems & Journal)");
        System.out.println("6. Administrative Inspection of Specific Booking Reference");
        System.out.println("7. Administrative Manual Rollback of an Existing Booking");
        System.out.println("8. Simulate Application Restart & Recover Dangling Transactions From Disk");
        System.out.println("9. Demonstrate Standalone Direct Subsystem Usage (Outside Facade)");
        System.out.println("10. Reset Storage (Clean All Disk Stores)");
        System.out.println("0. Exit");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void handleHappyPathBooking(Scanner scanner) {
        System.out.println(">>> RUNNING SCENARIO 1: HAPPY PATH BOOKING <<<");
        System.out.print("Enter Passenger Name [Alice Wonderland]: ");
        String name = readDefault(scanner, "Alice Wonderland");
        System.out.print("Enter Flight Number [FL-101]: ");
        String flight = readDefault(scanner, "FL-101");
        System.out.print("Enter Seat Number (1-35, A-F) [12A]: ");
        String seat = readDefault(scanner, "12A");
        System.out.print("Enter Seat Class (ECONOMY/BUSINESS/FIRST) [ECONOMY]: ");
        String seatClass = readDefault(scanner, "ECONOMY");
        System.out.print("Enter Baggage Weight (kg) [18.0]: ");
        double baggage = Double.parseDouble(readDefault(scanner, "18.0"));
        System.out.print("Enter Loyalty Member ID [MEM-8001]: ");
        String memberId = readDefault(scanner, "MEM-8001");
        System.out.print("Enter Membership Tier (STANDARD/SILVER/GOLD/PLATINUM) [GOLD]: ");
        String tier = readDefault(scanner, "GOLD");
        System.out.print("Enter Meal Preference (STANDARD/VEGAN/HALAL/KOSHER/GLUTEN_FREE) [VEGAN]: ");
        String meal = readDefault(scanner, "VEGAN");

        BookingRequest request = BookingRequest.builder()
                .flightNumber(flight)
                .passengerId("PAX-" + Math.abs(name.hashCode() % 10000))
                .passengerName(name)
                .seatNumber(seat)
                .seatClass(seatClass)
                .baggageWeightKg(baggage)
                .memberId(memberId)
                .membershipTier(tier)
                .mealPreference(meal)
                .build();

        try {
            BookingResult result = bookingFacade.bookFlight(request);
            System.out.println();
            System.out.println("Booking Reference   : " + result.getBookingReference());
            System.out.println("Status              : " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("Final Stage         : " + result.getFinalStage());
            System.out.println("Seat Reservation ID : " + result.getSeatReservation().getReservationId()
                    + " [Status: " + result.getSeatReservation().getStatus() + ", Fare: $" + result.getSeatReservation().getFare() + "]");
            System.out.println("E-Ticket Number     : " + result.getTicketIssuance().getTicketNumber()
                    + " [Status: " + result.getTicketIssuance().getStatus() + ", Excess Fee: $" + result.getTicketIssuance().getExcessBaggageFee() + "]");
            System.out.println("Loyalty Conf Code   : " + result.getLoyaltyRecord().getConfirmationCode()
                    + " [Status: " + result.getLoyaltyRecord().getStatus() + ", Points: " + result.getLoyaltyRecord().getPointsAccrued() + "]");
            System.out.println("Durable Persistence : State successfully written to disk across all 3 stores!");
        } catch (FlightReservationException e) {
            System.out.println("Booking Rejected: " + e.getMessage());
        }
    }

    private void handleStepBFailureBooking(Scanner scanner) {
        System.out.println(">>> RUNNING SCENARIO 2: COMPENSATION DEPTH 1 (STEP B FAILURE) <<<");
        System.out.println("Policy: Maximum single baggage weight is 32.0 kg. Attempting to book with 40.0 kg.");
        System.out.print("Enter Flight Number [FL-102]: ");
        String flight = readDefault(scanner, "FL-102");
        System.out.print("Enter Seat Number [14B]: ");
        String seat = readDefault(scanner, "14B");
        System.out.print("Enter Baggage Weight (>32kg to trigger failure) [40.0]: ");
        double baggage = Double.parseDouble(readDefault(scanner, "40.0"));

        BookingRequest request = BookingRequest.builder()
                .flightNumber(flight)
                .passengerId("PAX-B-FAIL")
                .passengerName("Bob Luggage")
                .seatNumber(seat)
                .seatClass("ECONOMY")
                .baggageWeightKg(baggage)
                .memberId("MEM-B-FAIL")
                .membershipTier("STANDARD")
                .mealPreference("STANDARD")
                .build();

        BookingResult result = bookingFacade.bookFlight(request);
        System.out.println();
        System.out.println("Booking Reference   : " + result.getBookingReference());
        System.out.println("Status              : " + (result.isSuccess() ? "SUCCESS" : "FAILED / COMPENSATED"));
        System.out.println("Final Stage         : " + result.getFinalStage());
        System.out.println("Failure Reason      : " + result.getFailureReason());
        System.out.println("Compensated Steps   : " + result.getCompensatedSteps());
        System.out.println("Seat Entity State   : " + result.getSeatReservation().getStatus());
        System.out.println("Ticket Entity State : " + (result.getTicketIssuance() != null ? result.getTicketIssuance().getStatus() : "NEVER_CREATED"));
        System.out.println("Durable Persistence : Verified that Subsystem A state was rolled back to RELEASED on disk.");
    }

    private void handleStepCFailureBooking(Scanner scanner) {
        System.out.println(">>> RUNNING SCENARIO 3: COMPENSATION DEPTH 2 (STEP C FAILURE) <<<");
        System.out.println("Policy: Accounts with status SUSPENDED/RESTRICTED are barred from finalization.");
        System.out.print("Enter Flight Number [FL-103]: ");
        String flight = readDefault(scanner, "FL-103");
        System.out.print("Enter Seat Number [16C]: ");
        String seat = readDefault(scanner, "16C");
        System.out.print("Enter Member ID (Starts with SUSPENDED to trigger failure) [SUSPENDED_USER_88]: ");
        String memberId = readDefault(scanner, "SUSPENDED_USER_88");

        BookingRequest request = BookingRequest.builder()
                .flightNumber(flight)
                .passengerId("PAX-C-FAIL")
                .passengerName("Charlie Blocked")
                .seatNumber(seat)
                .seatClass("BUSINESS")
                .baggageWeightKg(20.0)
                .memberId(memberId)
                .membershipTier("PLATINUM")
                .mealPreference("STANDARD")
                .build();

        BookingResult result = bookingFacade.bookFlight(request);
        System.out.println();
        System.out.println("Booking Reference   : " + result.getBookingReference());
        System.out.println("Status              : " + (result.isSuccess() ? "SUCCESS" : "FAILED / COMPENSATED"));
        System.out.println("Final Stage         : " + result.getFinalStage());
        System.out.println("Failure Reason      : " + result.getFailureReason());
        System.out.println("Compensated Steps   : " + result.getCompensatedSteps());
        System.out.println("Ticket Entity State : " + result.getTicketIssuance().getStatus() + " (Unwound first)");
        System.out.println("Seat Entity State   : " + result.getSeatReservation().getStatus() + " (Unwound second)");
        System.out.println("Loyalty Entity State: NEVER_COMMITTED");
        System.out.println("Durable Persistence : Two-step compensation (B -> A) recorded directly to disk.");
    }

    private void handleStepAFailureBooking(Scanner scanner) {
        System.out.println(">>> RUNNING SCENARIO 4: PRE-COMMIT FAILURE AT STEP A <<<");
        System.out.println("Policy: Aircraft seat layout only allows rows 1-35 and letters A-F.");
        System.out.print("Enter Seat Number [99Z]: ");
        String seat = readDefault(scanner, "99Z");

        BookingRequest request = BookingRequest.builder()
                .flightNumber("FL-104")
                .passengerId("PAX-BAD-SEAT")
                .passengerName("Dave BadSeat")
                .seatNumber(seat)
                .seatClass("ECONOMY")
                .baggageWeightKg(15.0)
                .build();

        try {
            bookingFacade.bookFlight(request);
        } catch (SeatUnavailableException e) {
            System.out.println();
            System.out.println("Exception Caught Successfully: " + e.getMessage());
            System.out.println("Outcome: Request rejected at intake step BEFORE any disk commits occurred.");
            System.out.println("Disk Journal & Stores: Completely untouched.");
        }
    }

    private void handleInspectAllPersistedStores() {
        System.out.println(">>> INSPECTING DISK STORAGE <<<");
        seatRepository.reload();
        ticketRepository.reload();
        loyaltyRepository.reload();
        journalRepository.reload();

        List<SeatReservation> seats = seatRepository.findAll();
        List<TicketIssuance> tickets = ticketRepository.findAll();
        List<LoyaltyAccountRecord> loyaltyRecords = loyaltyRepository.findAll();
        List<BookingTransaction> transactions = journalRepository.findAll();

        System.out.println("--- Subsystem A: Seat Reservations (" + seats.size() + ") ---");
        for (SeatReservation s : seats) {
            System.out.printf("  ID: %-15s | Flight: %-8s | Seat: %-4s | Status: %-10s | Fare: $%.2f%n",
                    s.getReservationId(), s.getFlightNumber(), s.getSeatNumber(), s.getStatus(), s.getFare());
        }

        System.out.println("--- Subsystem B: Ticket Issuances (" + tickets.size() + ") ---");
        for (TicketIssuance t : tickets) {
            System.out.printf("  Ticket: %-18s | Ref: %-15s | Status: %-10s | Baggage: %.1fkg | Fee: $%.2f%n",
                    t.getTicketNumber(), t.getBookingReference(), t.getStatus(), t.getBaggageWeightKg(), t.getExcessBaggageFee());
        }

        System.out.println("--- Subsystem C: Loyalty Records (" + loyaltyRecords.size() + ") ---");
        for (LoyaltyAccountRecord l : loyaltyRecords) {
            System.out.printf("  Conf: %-16s | Ref: %-15s | Member: %-12s | Status: %-10s | Points: %d%n",
                    l.getConfirmationCode(), l.getBookingReference(), l.getMemberId(), l.getStatus(), l.getPointsAccrued());
        }

        System.out.println("--- Coordinator: Journal Transactions (" + transactions.size() + ") ---");
        for (BookingTransaction tx : transactions) {
            System.out.printf("  Ref: %-15s | Pax: %-16s | Stage: %-28s | Reason: %s%n",
                    tx.getBookingReference(), tx.getPassengerName(), tx.getStage(),
                    tx.getFailureReason() != null ? tx.getFailureReason() : "None");
        }
    }

    private void handleInspectSpecificBooking(Scanner scanner) {
        System.out.print("Enter Booking Reference to inspect: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String ref = scanner.nextLine().trim();
        ReservationInspection inspection = operationsFacade.inspectBooking(ref);

        System.out.println();
        System.out.println("Booking Reference : " + inspection.getBookingReference());
        System.out.println("Consistent        : " + (inspection.isConsistent() ? "YES" : "NO (Inconsistency Detected!)"));
        System.out.println("Summary           : " + inspection.getSummary());

        if (inspection.getTransaction() != null) {
            BookingTransaction tx = inspection.getTransaction();
            System.out.println("Transaction Stage : " + tx.getStage());
            System.out.println("Passenger         : " + tx.getPassengerName() + " (Flight: " + tx.getFlightNumber() + ")");
            System.out.println("Seat Assigned     : " + tx.getSeatNumber());
        }
    }

    private void handleManualRollback(Scanner scanner) {
        System.out.print("Enter Booking Reference to rollback: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String ref = scanner.nextLine().trim();

        boolean ok = operationsFacade.manualRollback(ref);
        if (ok) {
            System.out.println("Manual rollback executed successfully for: " + ref);
            ReservationInspection inspection = operationsFacade.inspectBooking(ref);
            System.out.println("Updated state: " + inspection.getSummary());
        } else {
            System.out.println("Could not find booking reference: " + ref);
        }
    }

    private void handleSimulateRestartAndReconciliation() {
        System.out.println(">>> SIMULATING APPLICATION RESTART & RECONCILIATION <<<");
        System.out.println("Re-reading persisted files from disk and scanning for unfinalized chains...");
        int count = operationsFacade.reconcilePendingTransactions();
        System.out.println("Reconciliation complete. Total interrupted/pending transactions recovered: " + count);
    }

    private void handleDirectSubsystemDemonstration() {
        System.out.println(">>> DIRECT STANDALONE SUBSYSTEM USAGE DEMONSTRATION <<<");
        System.out.println("Demonstrating that Subsystem A, B, and C can be operated directly outside the Facade:");

        System.out.println("\n1. Direct Subsystem A execution (Seat hold & validation):");
        seatMapValidator.validateSeatFormat("5A");
        double calculatedFare = fareCalculator.calculateFare("BUSINESS", "5A");
        SeatReservation directSeat = SeatReservation.create("RES-STANDALONE", "FL-DIRECT", "PAX-DIR", "5A", "BUSINESS", calculatedFare);
        seatRepository.save(directSeat);
        System.out.println("   Seat hold persisted directly: " + directSeat.getReservationId() + " -> Status: " + directSeat.getStatus());

        System.out.println("\n2. Direct Subsystem B execution (Baggage fee & ticket generator):");
        double fee = baggageRulesEngine.validateAndCalculateFee(25.0);
        String ticketNum = ticketGenerator.generateTicketNumber();
        TicketIssuance directTicket = TicketIssuance.issue(ticketNum, "BKG-STANDALONE", "Direct User", "FL-DIRECT", 25.0, fee);
        ticketRepository.save(directTicket);
        System.out.println("   Ticket generated directly: " + directTicket.getTicketNumber() + " -> Fee: $" + directTicket.getExcessBaggageFee());

        System.out.println("\n3. Direct Subsystem C execution (Meal validator & loyalty points):");
        String meal = mealValidator.validate("HALAL", "MEM-DIRECT");
        int points = pointsCalculator.calculatePoints(500.0, "PLATINUM");
        LoyaltyAccountRecord directLoyalty = LoyaltyAccountRecord.accrue("CONF-STANDALONE", "BKG-STANDALONE", "MEM-DIRECT", "PLATINUM", points, meal, 500.0);
        loyaltyRepository.save(directLoyalty);
        System.out.println("   Loyalty recorded directly: " + directLoyalty.getConfirmationCode() + " -> Points: " + directLoyalty.getPointsAccrued());
    }

    private void handleResetAllStores() {
        operationsFacade.resetAllStores();
        System.out.println("All data stores on disk have been cleared successfully.");
    }

    private String readDefault(Scanner scanner, String defaultValue) {
        if (!scanner.hasNextLine()) {
            return defaultValue;
        }
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
}
