import os
import subprocess
import shutil

svg_path = os.path.join(os.path.dirname(__file__), "diagram.svg")
with open(svg_path, "r", encoding="utf-8") as f:
    svg_content = f.read()

svg_content = svg_content.replace('width="100%" height="100%"', 'width="100%" height="auto" style="max-width:100%; height:auto;"')

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>SDP Assignment 4 - Facade Over Three Subsystems Report</title>
<style>
  @page {{
    size: A4 portrait;
    margin: 16mm 16mm 16mm 16mm;
    @bottom-right {{
      content: "Page " counter(page);
      font-size: 8.5pt;
      font-family: 'Segoe UI', Arial, sans-serif;
      color: #64748b;
    }}
  }}

  body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1e293b;
    line-height: 1.5;
    font-size: 9.5pt;
    margin: 0;
    padding: 0;
  }}

  .header-card {{
    background: linear-gradient(135deg, #1e3a8a 0%, #0f172a 100%);
    color: white;
    padding: 22px 26px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
  }}

  .header-card h1 {{
    font-size: 19pt;
    margin: 0 0 6px 0;
    font-weight: 700;
    letter-spacing: -0.5px;
  }}

  .header-card h2 {{
    font-size: 12pt;
    font-weight: 400;
    margin: 0 0 14px 0;
    color: #93c5fd;
  }}

  .meta-grid {{
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    font-size: 9pt;
    border-top: 1px solid rgba(255,255,255,0.2);
    padding-top: 10px;
  }}

  .meta-item strong {{
    color: #bfdbfe;
  }}

  h2.section-title {{
    color: #1e3a8a;
    font-size: 13pt;
    border-bottom: 2px solid #3b82f6;
    padding-bottom: 4px;
    margin-top: 20px;
    margin-bottom: 10px;
    font-weight: 700;
    break-after: avoid;
  }}

  h3.subsection-title {{
    color: #0f766e;
    font-size: 10.5pt;
    margin-top: 14px;
    margin-bottom: 6px;
    font-weight: 600;
    break-after: avoid;
  }}

  p {{
    margin: 0 0 8px 0;
    text-align: justify;
  }}

  ul, ol {{
    margin: 0 0 10px 0;
    padding-left: 20px;
  }}

  li {{
    margin-bottom: 4px;
  }}

  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 10px 0 14px 0;
    font-size: 8.5pt;
    break-inside: avoid;
  }}

  th, td {{
    border: 1px solid #cbd5e1;
    padding: 6px 8px;
    text-align: left;
  }}

  th {{
    background-color: #f1f5f9;
    color: #1e293b;
    font-weight: 600;
  }}

  tr:nth-child(even) {{
    background-color: #f8fafc;
  }}

  .badge {{
    display: inline-block;
    padding: 2px 6px;
    font-size: 7.5pt;
    font-weight: 600;
    border-radius: 4px;
  }}

  .badge-success {{ background-color: #dcfce7; color: #166534; }}
  .badge-warning {{ background-color: #fef9c3; color: #854d0e; }}
  .badge-danger  {{ background-color: #fee2e2; color: #991b1b; }}
  .badge-info    {{ background-color: #e0f2fe; color: #075985; }}

  .callout {{
    padding: 9px 13px;
    border-left: 4px solid #3b82f6;
    background-color: #f0f9ff;
    border-radius: 0 6px 6px 0;
    margin: 10px 0 12px 0;
    font-size: 8.8pt;
    break-inside: avoid;
  }}

  .callout-warning {{
    border-left-color: #f59e0b;
    background-color: #fffbeb;
  }}

  .callout-success {{
    border-left-color: #10b981;
    background-color: #ecfdf5;
  }}

  pre {{
    background-color: #0f172a;
    color: #f8fafc;
    padding: 9px 12px;
    border-radius: 6px;
    font-family: Consolas, "Courier New", monospace;
    font-size: 7.8pt;
    line-height: 1.35;
    overflow-x: auto;
    margin: 8px 0 12px 0;
    break-inside: avoid;
  }}

  code {{
    font-family: Consolas, "Courier New", monospace;
    font-size: 8.2pt;
    background-color: #f1f5f9;
    color: #0f766e;
    padding: 1px 3px;
    border-radius: 3px;
  }}

  pre code {{
    background-color: transparent;
    color: inherit;
    padding: 0;
  }}

  .page-break {{
    page-break-before: always;
  }}

  .diagram-container {{
    text-align: center;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    padding: 10px;
    border-radius: 8px;
    margin: 10px 0;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
    break-inside: avoid;
  }}

  .footer-note {{
    margin-top: 18px;
    font-size: 8pt;
    color: #64748b;
    text-align: center;
    border-top: 1px solid #e2e8f0;
    padding-top: 8px;
  }}
</style>
</head>
<body>

  <!-- PAGE 1: TITLE & EXECUTIVE SUMMARY -->
  <div class="header-card">
    <h1>Software Design Patterns (SDP) — Assignment 4</h1>
    <h2>Facade Over a Three-Stage Chain, With Persistence and a Live Demo</h2>
    <div class="meta-grid">
      <div class="meta-item"><strong>Student:</strong> Nodir Mukhammedov</div>
      <div class="meta-item"><strong>Group:</strong> SE-2536</div>
      <div class="meta-item"><strong>Assigned Domain:</strong> Variant 8 — Flight Reservation</div>
      <div class="meta-item"><strong>GitHub Repository:</strong> https://github.com/Eqziz/SDP-assignment-4.git</div>
    </div>
  </div>

  <h2 class="section-title">1. Executive Summary &amp; System Architecture</h2>
  <p>
    Modern enterprise systems frequently require coordinating multiple autonomous subsystems behind a unified business transaction. In this project, a complete, highly durable <strong>Flight Reservation System</strong> is designed and implemented using the <strong>Facade Pattern</strong>.
  </p>
  <p>
    The system coordinates three autonomous subsystems in a sequential chain:
  </p>
  <ul>
    <li><strong>Step A (Intake / Seat Reservation):</strong> Validates seat maps, computes class/location pricing, and establishes a durable seat hold (<code>HELD</code>).</li>
    <li><strong>Step B (Processing / Execution):</strong> Enforces baggage policies, assesses excess weight fees, generates official IATA ticket serial numbers, and issues e-tickets (<code>ISSUED</code>).</li>
    <li><strong>Step C (Finalization / Confirmation):</strong> Validates passenger loyalty eligibility and catering meal restrictions, credits frequent-flyer miles, and finalizes the reservation (<code>SETTLED</code>).</li>
  </ul>
  <p>
    The core architectural challenge is handling partial failures: real chains break midway after earlier steps have already written committed state to durable disk storage. The Facade orchestrates the execution, logs progress into a persistent transaction journal, detects downstream failures, and reliably unwinds the chain across two distinct compensation depths.
  </p>

  <div class="callout callout-success">
    <strong>Mandatory Constraints Fully Satisfied:</strong>
    All three subsystems are completely decoupled (zero cross-subsystem imports), state transitions are strictly self-enforced by domain entities, state persists durably across JVM restarts using JSON files, unit test coverage exceeds 94%, and all source code is strictly written without comments.
  </div>

  <!-- PAGE 2: SUBSYSTEMS BREAKDOWN -->
  <h2 class="section-title page-break">2. Independent Subsystems Breakdown</h2>
  <p>
    Each subsystem functions as a self-contained domain module consisting of 4 to 5 classes. Each subsystem features dedicated pure business logic classes separate from its entity, enforces its own state transition rules, and communicates directly with its own embedded JSON file store. No subsystem imports or knows about any other subsystem.
  </p>

  <table>
    <thead>
      <tr>
        <th style="width:20%;">Subsystem</th>
        <th style="width:22%;">Class Name</th>
        <th style="width:33%;">Type / Responsibility</th>
        <th style="width:25%;">Durable File Gateway</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td rowspan="4"><strong>Subsystem A</strong><br>(Seat Reservation)</td>
        <td><code>SeatReservation</code></td>
        <td>Domain Entity (States: <span class="badge badge-info">HELD</span>, <span class="badge badge-success">CONFIRMED</span>, <span class="badge badge-danger">RELEASED</span>)</td>
        <td rowspan="4"><code>SeatReservationRepository</code><br>(<code>data/seat_reservations.json</code>)</td>
      </tr>
      <tr>
        <td><code>FareCalculator</code></td>
        <td>Pure Business Logic (Cabin rates, exit rows, taxes)</td>
      </tr>
      <tr>
        <td><code>SeatMapValidator</code></td>
        <td>Pure Business Logic (Aircraft seat grid &amp; conflict checks)</td>
      </tr>
      <tr>
        <td><code>SeatReservationStatus</code></td>
        <td>State Enum with legal transition enforcement</td>
      </tr>
      <tr>
        <td rowspan="4"><strong>Subsystem B</strong><br>(Ticket &amp; Baggage)</td>
        <td><code>TicketIssuance</code></td>
        <td>Domain Entity (States: <span class="badge badge-info">ISSUED</span>, <span class="badge badge-success">COMPLETED</span>, <span class="badge badge-danger">VOIDED</span>)</td>
        <td rowspan="4"><code>TicketIssuanceRepository</code><br>(<code>data/ticket_issuances.json</code>)</td>
      </tr>
      <tr>
        <td><code>BaggageRulesEngine</code></td>
        <td>Pure Business Logic (20kg allowance, 32kg max safety limit)</td>
      </tr>
      <tr>
        <td><code>TicketNumberGenerator</code></td>
        <td>Pure Business Logic (IATA standard format: <code>ETKT-2026-XXXXXX</code>)</td>
      </tr>
      <tr>
        <td><code>TicketStatus</code></td>
        <td>State Enum with legal transition enforcement</td>
      </tr>
      <tr>
        <td rowspan="4"><strong>Subsystem C</strong><br>(Loyalty &amp; Catering)</td>
        <td><code>LoyaltyAccountRecord</code></td>
        <td>Domain Entity (States: <span class="badge badge-info">ACCRUED</span>, <span class="badge badge-success">SETTLED</span>, <span class="badge badge-danger">REVOKED</span>)</td>
        <td rowspan="4"><code>LoyaltyRecordRepository</code><br>(<code>data/loyalty_records.json</code>)</td>
      </tr>
      <tr>
        <td><code>LoyaltyPointsCalculator</code></td>
        <td>Pure Business Logic (Tier multipliers: Standard to Platinum)</td>
      </tr>
      <tr>
        <td><code>MealPreferenceValidator</code></td>
        <td>Pure Business Logic (Catering policy &amp; account suspension checks)</td>
      </tr>
      <tr>
        <td><code>LoyaltyStatus</code></td>
        <td>State Enum with legal transition enforcement</td>
      </tr>
    </tbody>
  </table>

  <!-- PAGE 3: THE DUAL FACADE & PERSISTENCE -->
  <h2 class="section-title page-break">3. The Dual Facade Architecture</h2>
  <p>
    Two distinct facades encapsulate the trio of subsystems, catering to different client personas and operational responsibilities:
  </p>

  <h3 class="subsection-title">1. FlightBookingFacade (Primary Client Workflow)</h3>
  <p>
    Provides the standard client entry point: <code>BookingResult bookFlight(BookingRequest request)</code>. Rather than acting as a superficial delegate, this facade contains real sequencing and transactional decision logic:
  </p>
  <ul>
    <li><strong>Input &amp; Pre-Commit Validation:</strong> Validates passenger credentials and ensures the requested seat is structurally valid and unoccupied before writing any records to disk.</li>
    <li><strong>Step A (Seat Hold):</strong> Computes cabin pricing, generates reservation identifiers, transitions seat entity to <code>HELD</code>, and commits to disk.</li>
    <li><strong>Step B (Ticket Issuance):</strong> Evaluates baggage limits. If valid, generates an e-ticket and commits to disk. If baggage exceeds 32.0 kg, intercepts <code>ExcessBaggageException</code> and executes <strong>Compensation Depth 1</strong>.</li>
    <li><strong>Step C (Loyalty Confirmation):</strong> Validates meal preferences and checks for suspended accounts. If verified, computes points, commits loyalty records, and promotes all three subsystem entities to their final settled states (<code>CONFIRMED</code>, <code>COMPLETED</code>, <code>SETTLED</code>). If an account is suspended, intercepts <code>LoyaltyProcessingException</code> and executes <strong>Compensation Depth 2</strong>.</li>
  </ul>

  <h3 class="subsection-title">2. FlightOperationsFacade (Administrative &amp; Recovery Workflow)</h3>
  <p>
    Provides operations, manual intervention, and audit capabilities:
  </p>
  <ul>
    <li><strong><code>inspectBooking(bookingReference):</code></strong> Reads correlated records from all three subsystem repositories and the journal to verify cross-system atomic consistency.</li>
    <li><strong><code>reconcilePendingTransactions():</code></strong> Post-crash recovery engine. Reads the journal from disk after an unexpected JVM termination, detects unfinished/interrupted chains, and safely executes rollbacks.</li>
    <li><strong><code>manualRollback(bookingReference):</code></strong> Allows authorized administrators to manually unwind confirmed or partially completed bookings.</li>
    <li><strong><code>resetAllStores():</code></strong> Completely purges all disk files for repeatable demonstrations.</li>
  </ul>

  <h2 class="section-title">4. Durable Persistence &amp; Crash Recovery</h2>
  <p>
    Persistence is real and durable: all subsystem repositories serialize domain entities to JSON files via Jackson Databind and JSR-310 datetime modules. To survive application restarts mid-chain without relying on volatile in-memory state, the chain's execution progress itself is durably journaled:
  </p>

  <div class="callout callout-info">
    <strong>Persisted Chain Lifecycle Stages:</strong><br>
    <code>INITIALIZED</code> &rarr; <code>STEP_A_SEAT_HELD</code> &rarr; <code>STEP_B_TICKET_ISSUED</code> &rarr; <code>COMPLETED</code><br>
    <em>On Step B Failure:</em> <code>COMPENSATING_AFTER_B_FAILURE</code> &rarr; <code>COMPENSATED_A_ONLY</code><br>
    <em>On Step C Failure:</em> <code>COMPENSATING_AFTER_C_FAILURE</code> &rarr; <code>COMPENSATED_FULL</code>
  </div>

  <p>
    When the system restarts, <code>FlightOperationsFacade.reconcilePendingTransactions()</code> reads <code>data/booking_journal.json</code> directly from disk. Any transactions found in an intermediate state (e.g. <code>STEP_A_SEAT_HELD</code> or <code>STEP_B_TICKET_ISSUED</code>) are automatically identified, and their persisted entities in Subsystems A and B are unwound to clean terminal states (<code>RELEASED</code> and <code>VOIDED</code>).
  </p>

  <!-- PAGE 4: CONSISTENCY & COMPENSATION DEPTHS -->
  <h2 class="section-title page-break">5. Cross-Subsystem Consistency &amp; Two-Depth Compensation</h2>
  <p>
    The system models two realistic, domain-specific failure scenarios that mandate rollback:
  </p>

  <h3 class="subsection-title">Compensation Depth 1: Step B Fails After Step A Committed</h3>
  <p>
    <strong>Domain Trigger:</strong> Passenger requests a checked bag weighing 40.0 kg. The airline's maximum single-bag safety limit is 32.0 kg. Step A has already committed seat hold <code>14B</code> as <code>HELD</code>. Step B's <code>BaggageRulesEngine</code> rejects the baggage with <code>ExcessBaggageException</code>.
  </p>
  <p>
    <strong>Unwind Sequence:</strong> The facade catches the exception, updates the transaction stage to <code>COMPENSATING_AFTER_B_FAILURE</code>, and compensates Subsystem A alone: <code>SeatReservation.transitionTo(RELEASED)</code> is invoked and written to disk. Journal advances to <code>COMPENSATED_A_ONLY</code>.
  </p>

  <h3 class="subsection-title">Compensation Depth 2: Step C Fails After Steps A and B Committed</h3>
  <p>
    <strong>Domain Trigger:</strong> Passenger holds seat <code>16C</code> (<code>HELD</code>) and ticket <code>ETKT-2026-100001</code> is successfully issued (<code>ISSUED</code>). However, during Step C, the passenger's member ID is flagged as <code>SUSPENDED_USER_88</code>. <code>MealPreferenceValidator</code> rejects the request with <code>LoyaltyProcessingException</code>.
  </p>
  <p>
    <strong>Two-Step Unwind Sequence:</strong> The facade updates the journal to <code>COMPENSATING_AFTER_C_FAILURE</code> and unwinds the chain in reverse order:
  </p>
  <ol>
    <li><strong>Step 1:</strong> Compensates Subsystem B &rarr; <code>TicketIssuance.transitionTo(VOIDED)</code> and updates disk.</li>
    <li><strong>Step 2:</strong> Compensates Subsystem A &rarr; <code>SeatReservation.transitionTo(RELEASED)</code> and updates disk.</li>
    <li>Journal advances to <code>COMPENSATED_FULL</code>.</li>
  </ol>

  <h3 class="subsection-title">Pre-Commit Validation Failure at Step A</h3>
  <p>
    If seat format is invalid (e.g., <code>99Z</code>) or seat is already occupied on that flight, <code>SeatMapValidator</code> throws <code>SeatUnavailableException</code> before any records are committed to disk, keeping storage pristine.
  </p>

  <!-- PAGE 5: LEAST KNOWLEDGE (DEMETER) -->
  <h2 class="section-title page-break">6. Principle of Least Knowledge (Law of Demeter)</h2>
  <p>
    The Principle of Least Knowledge dictates that a software component should only interact with its immediate friends and avoid traversing complex subsystem graphs.
  </p>

  <h3 class="subsection-title">Before Facade: Deep Chains &amp; Massive Coupling</h3>
  <p>
    Without the facade, a client booking a flight would need direct coupling to at least 10 classes across three disparate subsystems. The client would be forced to coordinate sequencing, track multiple repositories, and manually execute reverse rollbacks when downstream checks fail:
  </p>
  <pre><code>SeatMapValidator validator = new SeatMapValidator();
FareCalculator fareCalc = new FareCalculator();
SeatReservationRepository seatRepo = new SeatReservationRepository(seatPath);

validator.validateSeatFormat("12A");
validator.ensureSeatAvailable("AA101", "12A", seatRepo);
SeatReservation seat = SeatReservation.create("RES-1", "AA101", "PAX-1", "12A", "ECONOMY", 240.0);
seatRepo.save(seat);

TicketIssuanceRepository ticketRepo = new TicketIssuanceRepository(ticketPath);
BaggageRulesEngine baggageEngine = new BaggageRulesEngine();
TicketIssuance ticket = null;
try {{
    double fee = baggageEngine.validateAndCalculateFee(40.0);
    ticket = TicketIssuance.issue("ETKT-1", "BKG-1", "Alice", "AA101", 40.0, fee);
    ticketRepo.save(ticket);
}} catch (ExcessBaggageException ex) {{
    seat.transitionTo(SeatReservationStatus.RELEASED);
    seatRepo.update(seat);
    throw ex;
}}</code></pre>

  <h3 class="subsection-title">After Facade: Single Immediate Friend</h3>
  <p>
    With the facade layer in place, the client interacts solely with <code>FlightBookingFacade</code>, passing a strongly-typed <code>BookingRequest</code> and receiving an atomic <code>BookingResult</code>:
  </p>
  <pre><code>FlightBookingFacade facade = getBookingFacade();
BookingRequest request = BookingRequest.builder()
    .flightNumber("AA101").passengerName("Alice").seatNumber("12A")
    .baggageWeightKg(18.0).memberId("MEM-101").mealPreference("VEGAN")
    .build();

BookingResult result = facade.bookFlight(request);
if (result.isSuccess()) {{
    System.out.println("Flight Confirmed: " + result.getBookingReference());
}} else {{
    System.out.println("Booking Compensated: " + result.getFailureReason());
}}</code></pre>

  <!-- PAGE 6: REFLECTION: FACADE VS. ADAPTER -->
  <h2 class="section-title page-break">7. Written Reflection: Facade vs. Adapter</h2>
  <div class="callout callout-warning">
    <strong>Architectural Intent Comparison:</strong><br>
    The decision between Facade and Adapter must be argued strictly from architectural intent, not class count.
  </div>
  <p>
    <strong>Adapter's Intent: Interface Translation.</strong> An Adapter wraps an existing, incompatible class to make it conform to an expected target interface. Its sole purpose is polymorphic substitutability: allowing a client to utilize a foreign or legacy component without modifying the client's code or the component's internal behavior.
  </p>
  <p>
    <strong>Facade's Intent: Subsystem Encapsulation &amp; Orchestration.</strong> A Facade provides a higher-level, simplified interface to an entire subsystem or cluster of subsystems. Rather than converting one interface into another, it introduces a brand new interface that encapsulates complex sequencing, multi-step coordination, transactional boundaries, and error recovery policies.
  </p>
  <p>
    <strong>Why This Problem Calls for a Facade:</strong> In the Flight Reservation domain, Subsystems A, B, and C are not alternative implementations of a single concept; <code>SeatReservation</code> cannot and should not be adapted into a <code>TicketIssuance</code>. None of the subsystems are trying to adhere to a common interface. Instead, they represent distinct, cooperating capabilities that must execute in a strict sequential chain with cross-system rollback capabilities. The client does not seek interface compatibility; it requires an orchestrator that presents a single atomic booking operation while handling partial rollbacks behind the scenes. Therefore, Facade is the architecturally sound pattern, while Adapter is conceptually inapplicable.
  </p>

  <!-- PAGE 7: UML CLASS DIAGRAM -->
  <h2 class="section-title page-break">8. Complete System UML Class Diagram</h2>
  <p>
    The vector diagram below illustrates the complete architecture, showing the Client (<code>FlightReservationApp</code>), the dual Facades (<code>FlightBookingFacade</code>, <code>FlightOperationsFacade</code>), the Coordinator journal, and all three decoupled subsystems with their associations:
  </p>

  <div class="diagram-container">
    {svg_content}
  </div>

  <!-- PAGE 8: VERIFICATION & TESTING -->
  <h2 class="section-title page-break">9. Verification, Test DSL &amp; Code Coverage</h2>
  <p>
    The codebase includes <strong>36 unit tests</strong> adhering to the F.I.R.S.T. testing principles (Fast, Independent, Repeatable, Self-Validating, Timely). Each test starts from a clean persisted state via JUnit 5 temporary directories (<code>@TempDir</code>).
  </p>

  <h3 class="subsection-title">Domain-Specific Testing Language (DSL)</h3>
  <p>
    A custom DSL helper (<code>FlightBookingDsl</code>) allows test scenarios to read as natural sentences in the flight domain:
  </p>
  <pre><code>FlightBookingDsl.given(bookingFacade, operationsFacade)
    .forPassenger("PAX-002", "Heavy Luggage Passenger")
    .flying("AA102")
    .requestingSeat("14B", "ECONOMY")
    .withBaggage(35.0)
    .withLoyalty("MEM-777", "STANDARD", "STANDARD")
    .whenBooking()
    .thenExpectFailureAtStepB()
    .andVerifyCompensatedAOnly();</code></pre>

  <h3 class="subsection-title">JaCoCo Code Coverage Report</h3>
  <table>
    <thead>
      <tr>
        <th>Package / Layer</th>
        <th>Total Lines</th>
        <th>Covered Lines</th>
        <th>Line Coverage (%)</th>
        <th>Status</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><code>com.flightreservation.facade</code></td>
        <td>293</td>
        <td>287</td>
        <td><strong>98.0%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr>
        <td><code>com.flightreservation.coordinator</code></td>
        <td>119</td>
        <td>109</td>
        <td><strong>91.6%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr>
        <td><code>com.flightreservation.subsystem.seat</code></td>
        <td>137</td>
        <td>126</td>
        <td><strong>92.0%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr>
        <td><code>com.flightreservation.subsystem.ticket</code></td>
        <td>109</td>
        <td>100</td>
        <td><strong>91.7%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr>
        <td><code>com.flightreservation.subsystem.loyalty</code></td>
        <td>118</td>
        <td>109</td>
        <td><strong>92.4%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr>
        <td><code>com.flightreservation.exception</code></td>
        <td>18</td>
        <td>18</td>
        <td><strong>100.0%</strong></td>
        <td><span class="badge badge-success">Passed</span></td>
      </tr>
      <tr style="font-weight:bold; background-color:#e2e8f0;">
        <td>TOTAL BUNDLE</td>
        <td>794</td>
        <td>749</td>
        <td><strong>94.3%</strong></td>
        <td><span class="badge badge-success">&gt;80% Goal Exceeded</span></td>
      </tr>
    </tbody>
  </table>

  <h2 class="section-title">10. Live Interactive Demo &amp; Commands</h2>
  <p>
    The system includes an interactive console application (<code>FlightReservationApp</code>) that allows the examiner to trigger the entire chain live, inspect disk stores, simulate application restarts, and verify rollbacks:
  </p>
  <ul>
    <li><strong>Run all unit tests:</strong> <code>mvn clean test</code></li>
    <li><strong>Launch live demo CLI:</strong> <code>mvn compile exec:java</code></li>
    <li><strong>Reset storage files:</strong> Choose menu option <code>10</code> or delete contents of <code>data/</code>.</li>
  </ul>

  <div class="footer-note">
    Report prepared for SDP Assignment 4 &bull; Student: Nodir Mukhammedov (SE-2536) &bull; Variant 8: Flight Reservation
  </div>

</body>
</html>
"""

html_path = os.path.join(os.path.dirname(__file__), "report.html")
with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print("Generated report.html successfully!")

chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
pdf_path = os.path.join(os.path.dirname(__file__), "Flight_Reservation_Facade_Assignment4_Report.pdf")
alt_pdf_path = os.path.join(os.path.dirname(__file__), "report.pdf")

cmd = [
    chrome_path,
    "--headless=new",
    "--disable-gpu",
    "--run-all-compositor-stages-before-draw",
    "--no-pdf-header-footer",
    f"--print-to-pdf={pdf_path}",
    f"file:///{os.path.abspath(html_path).replace(os.sep, '/')}"
]

print("Running Chrome to generate PDF...")
res = subprocess.run(cmd, capture_output=True, text=True)
print("Chrome stdout:", res.stdout)
print("Chrome stderr:", res.stderr)

if os.path.exists(pdf_path):
    shutil.copy(pdf_path, alt_pdf_path)
    print(f"PDF generated successfully at: {pdf_path}")
    print(f"Also copied to: {alt_pdf_path}")
    print(f"File size: {os.path.getsize(pdf_path)} bytes")
else:
    print("PDF generation failed!")
