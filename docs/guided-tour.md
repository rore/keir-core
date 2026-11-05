<a id="how-keir-works-a-guided-tour"></a>
<a id="what-keir-does"></a>
# How Keir works

Keir is a framework for adding customer-defined rules to an application. A developer decides which events and operations the application exposes. Customers can use those operations to define behavior that the application has no built-in setting for.

For example, a task app already knows when a task is completed and how to create a task. A customer wants to combine those features into a rule:

> When a task is completed in the launch project, create a follow-up task.

<a id="host-design-time"></a>
## Define what a rule can do

The application developer adds a **hook**, a point where the application can run a rule. This hook runs after a task is completed and passes the task’s ID to the rule. The developer exposes two operations the rule can call: read a task and create a task.

<a id="customer-extension-design"></a>
## Write and review a rule

An AI assistant can write a Keir rule from the customer’s request. The rule reads the completed task, checks its project, and requests a follow-up when the project matches. Keir’s compiler checks the rule against the application’s contract, including which operations it can call and what data they accept.

Before enabling the rule, the customer can review a simulation. A completed launch task should get a follow-up. A completed task in another project should leave the task list unchanged. The simulation uses sample data to show what the rule would request.

<a id="runtime"></a>
## Run the approved rule

When a task is completed, the application passes the event to Keir. Keir runs the compiled rule and passes any requested operation back to the application. The application checks permissions and performs the operation through its own services. If follow-up creation fails, the original task remains completed.

[Run the example](quickstart.md) · [Read the integration contracts](specs/README.md)
