# Keir rules by example

An extension is a custom rule in an application. Keir's language lets it combine inputs, conditions and calls to operations the app exposes. The app supplies its business data and actions, plus any AI assistant and review screens.

These are **logic sketches, not Keir syntax**. Booking, support and expense apps below are illustrative hosts, not implemented integrations. Daymark is an implemented **task-management demo app**; its tasks belong to Daymark.

These examples work with [released packages](installation.md); example 5 needs Core 0.5.0 or a later explicitly compatible release. Each host must expose the necessary data and operations.

## 1. Choose an answer from input

A booking app supplies whether a room permits guest bookings.

```text
Before a guest booking is saved:

  allowed ← room permits guest bookings

  IF allowed:
    ALLOW continuation
  ELSE:
    DENY with "Choose a room that allows guests"
```

**Shows:** name a yes/no value and use it to choose a decision and message. No service call is needed here. ALLOW lets the app continue its own checks; the app still handles permissions, availability and saving the reservation.

## 2. Use an operation's returned value

A support app could expose its customer-plan lookup before accepting a priority request.

```text
plan ← READ the customer's plan from the app

IF plan includes priority support:
  ALLOW the priority request
ELSE:
  DENY with "Priority support is not included"
```

**Shows:** an earlier operation's result can supply a value for a later condition. This read always runs, so its successful result is available afterward. A failed required lookup blocks continuation instead of producing an ALLOW.

## 3. Call an operation only when needed

Daymark exposes task completion and its task-reading and creation services.

```text
After Daymark saves a task completion:

  task ← READ the triggering task from Daymark
  makeFollowup ← task is completed in the selected project

  CREATE a follow-up task ONLY WHEN makeFollowup
```

**Shows:** save a condition, then use it to control whether an operation runs. A false condition skips creation. This sketch does not use the creation's returned value. The original completion is already saved; failure to create the follow-up does not undo it. The [Daymark walkthrough](guided-tour.md) shows this implemented demo flow.

## 4. Use the result of a conditional lookup

An expense app could supply a yes/no fact saying whether a claim needs an approval check, plus a lookup returning one of two approval statuses: "approved" or "not approved".

```text
needsCheck ← claim needs an approval check

approval ← LOOK UP employee approval
           ONLY WHEN needsCheck

Decision:
  IF needsCheck:
    IF approval.status is "not approved":
      DENY with "Manager approval is required"
    ELSE:
      ALLOW continuation
  ELSE:
    ALLOW continuation without reading approval
```

**Shows:** use the actual returned status in a condition even though the lookup is conditional. The same saved condition, `needsCheck`, controls both the lookup and the outer decision branch. The status is read only inside that branch's true side; Keir uses that exact match to check that a result is available before it is read. A different or combined test is not a replacement.

When `needsCheck` is false, no lookup happens and no result is read. If the lookup runs but fails, execution fails and the app blocks continuation; it does not take the skipped-lookup ALLOW path.

## 5. Apply a percentage with explicit rounding

An illustrative quoting app supplies an amount and a discount rate. With the qualified Core 0.5 numeric profile:

```text
subtotal ← 199.95
discountRate ← 0.075 (7.5%)
amount ← subtotal × (1 − discountRate)
amount ← round to two decimal places, ties to even
Result: 184.95
```

**Shows:** exact Decimal arithmetic and a chosen rounding rule, not binary floating-point or silent rounding. Integer quantities can also drive thresholds, such as requesting 3 items when stock is 7 and the target is 10. The [runnable stock and quote examples](../examples/numeric/README.md) include complete models, source, scenarios and Java/Node commands. Core does not enforce currencies, units or accounting policy; those belong to the host. See [Core 0.5.0](releases/core-0.5.0.md) for supported profiles and release qualification.

## From a sketch to a running extension

The app's assistant can help draft the extension. Keir compiles it against the app's declared capabilities. The app supplies example inputs and simulated service responses for review, records approval and controls activation. Keir then runs the compiled version through the app's services without another AI request. Those review cases check the selected situations, not every possible situation.

Before-save decisions are read-only. An extension responding after an event may contain at most one call that can change data. The app still enforces permissions and current business facts when saving, and checks uncertain outcomes before retrying.

Next: [see the Daymark demo](guided-tour.md), [run and change an example](quickstart.md), or [embed Keir in an application](embedding.md).
