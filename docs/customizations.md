# What can you customize with Keir?

Developers add Keir to an application so its users can configure custom rules, called extensions. The app can be about expenses, bookings, customer support or another domain. Its developers choose when rules run and which information and actions they may use.

The app supplies the authoring and review experience, including any AI assistant. Keir compiles the rules and runs the checked version through the app's services.

The expense, booking and support examples below are **illustrations, not implemented integrations**. **Daymark is a working task-management demo app** used to demonstrate Keir; its task features belong to Daymark.

## Expense app: check a submission

> For client expenses, require a receipt before submission.

An expense app could supply the expense category and whether a receipt is attached. The rule could stop submission when the receipt is missing, so the user can correct it. Passing the rule lets the app continue its normal permission, validation and save checks; it does not confirm that the expense was saved.

## Booking app: check a room's guest policy

> Before a guest books a room, check that the room permits guest bookings.

A booking app could expose a way to read the room's guest policy. The rule could use that answer to allow or stop the request. If the required lookup fails, the check blocks continuation. The app still handles availability, permissions and the actual reservation.

## Daymark demo: respond to a completed task

> When I complete a task in this project, create a follow-up task.

This is an implemented example in Daymark, the task-management demo. Daymark exposes task completion and its own task-reading and creation services. The rule checks the completed task's project and requests a follow-up only when it matches. The completion has already been saved; a problem creating the follow-up does not undo it.

The [screenshot walkthrough](guided-tour.md) shows this request through review and execution.

## Support app: use a newly assigned reference

> If I choose “Create a new case,” create it and give me its reference. Otherwise, keep the existing case reference.

The app would pass the user's choice and existing reference to the rule and expose its own case-creation service. The rule could return the reference that service actually assigned, or keep the existing reference when creation was intentionally skipped. A failed or uncertain creation is reported as such. If the response is lost, the app must check whether the case was created before retrying.

**The support app is hypothetical.** The underlying returned-reference feature has been tested in the **Keir Core 0.4.0 candidate**, the compiler and runtime packages. This candidate is unreleased; published Core 0.3.1 packages and Daymark do not support the feature. [Technical version details and evidence](agents/authoring-companion.md#development-edition-8) describe the tested generic-record examples.

## From a request to a running rule

In an application with an authoring assistant, the flow is:

1. **Describe it.** An AI assistant helps draft the rule using the app's exposed building blocks. It can ask about missing business choices.
2. **Compile it.** Keir checks that the draft fits the app's declared operations, data types and execution limits.
3. **Check and review it.** The app supplies example inputs and simulated service responses. Keir runs the rule against them; the user reviews expected and observed behavior, including cases that should do nothing or fail, and approves that version. These checks cover the selected cases. The app controls activation separately.
4. **Run it.** When the app invokes the approved rule, Keir executes the compiled version through the app's services, without another AI request.

Rules that respond after an event, like the Daymark example, may include at most one call to an app operation that can change data. Before-save rules, like the expense check, are read-only. The app defines each operation and owns its permissions, saving and outcome reporting.

Continue with the [Daymark demo walkthrough](guided-tour.md), [run and change an example](quickstart.md), or read the [embedding guide](embedding.md) to add Keir to an application.
