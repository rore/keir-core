# What can you customize with Keir?

People using the same application often want different rules: when to create a follow-up, what to check before saving, or which information to use in a decision. Keir lets an application offer those customizations through rules its users can describe and review.

The application supplies the tasks, customers or cases, and chooses which data and operations a rule may use. The examples below show what you could ask for in an application that exposes the necessary building blocks.

## Respond when something happens

> When I complete a task in this project, create a follow-up task called “Review the delivery.”

The rule checks the completed task's project and requests a follow-up only when it matches. Other completions leave the task list alone. The original completion has already been saved; a problem creating the follow-up does not undo it.

This is the kind of customization demonstrated in the [Daymark walkthrough](guided-tour.md).

## Check a change before saving

> Require a due date before adding a task tagged “client.”

The rule examines the proposed task. If its due date is missing, the app keeps the draft open so the user can correct it. Otherwise, the app continues its normal permission, validation and save checks. Passing the rule is not confirmation that the task was saved.

Daymark supports this [before-save check](https://github.com/rore/keir-demo-app/blob/main/docs/task-effects.md#checks-before-adding).

## Use information the application exposes

> Before accepting a priority support request, check whether the customer's plan includes priority support.

This is an illustrative support-app rule. The app would expose a way to read that customer's plan, and the rule could use the answer to allow or deny the request. If that required lookup fails, the check blocks continuation instead of assuming the customer is eligible.

## Use a newly assigned reference

> If I asked for a new case, create it and return its reference. Otherwise, keep the existing case reference.

Here, an illustrative support app could return the reference its case service actually assigned. When creation is deliberately skipped, it keeps the reference supplied at the start. A failed or uncertain creation is reported as such. The existing reference is used only when creation was intentionally skipped. If the response is lost, the app must check whether the case was created before retrying.

Using the result of a conditional operation this way is a **qualified Core 0.4.0 candidate capability**. It is not in the published Core 0.3.1 packages or Daymark's current integration. The [checked record example](../examples/README.md#guarded-result-scalar-dataflow) and [candidate qualification](../roadmap/features/guarded-result-dataflow.md#notes) provide the underlying evidence.

## From a request to a running rule

The application supplies the authoring and review experience:

1. **Describe it.** An AI assistant helps draft the rule using the app's exposed building blocks. It can ask about missing business choices.
2. **Compile it.** Keir checks that the draft fits the app's declared operations, data types and execution limits.
3. **Check and review it.** Run simulated examples, including cases that should do nothing or fail. These cover chosen cases; the user reviews the behavior and approves that version. The app separately controls when it is turned on.
4. **Run it.** When the app invokes the approved rule, Keir executes the compiled version through the app's services, without another AI request.

The app remains responsible for permissions, saving data and reporting what actually happened. Event rules can request at most one data-changing operation per run; before-save checks are read-only. The app chooses when rules run and which operations they can call.

Continue with the [screenshot walkthrough](guided-tour.md), [run and change an example](quickstart.md), or read the [embedding guide](embedding.md) to add these building blocks to an application.
