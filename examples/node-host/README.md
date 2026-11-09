# Node.js host example

[Run the follow-up extension in Node.js](../../docs/quickstart.md#run-the-precompiled-example). The [host](follow-up/run.mjs) binds simulated task services, prepares the compiled extension, and runs it for three task completions.

[Run the pre-operation decision](../../docs/quickstart.md#try-a-pre-operation-decision). Its [host](pre-operation/run.mjs) uses the same binding and invocation APIs, then consumes ALLOW, DENY or failed evaluation before counting simulated native continuation. The [source](pre-operation/source.yaml) uses the capabilities declared by its [model](pre-operation/dcm.json).

[Embed Keir in your application](../../docs/embedding.md) · [Node.js API](../../docs/specs/host-model-binding.md#api-shapes)

<a id="examples-for-earlier-profiles"></a>
<a id="implementation-reference-for-earlier-examples"></a>
<a id="effect3-qualification"></a>
<a id="qualified-write-example"></a>
<a id="read1-example"></a>
<a id="original-read-only-example"></a>
<a id="effect1-simulation"></a>
<a id="original-simulated-write"></a>
<a id="read2-and-effect2-examples"></a>
<a id="catalog-bound-read-and-simulated-write-examples"></a>
[Earlier Node.js examples](https://github.com/rore/keir-core/blob/8723d656123ec5c3016b50f4ecd60cd8413e9b23/examples/node-host/README.md)
