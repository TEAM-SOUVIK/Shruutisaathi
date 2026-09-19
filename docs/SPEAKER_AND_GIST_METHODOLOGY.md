# Speaker and Gist Methodology

Date: 2026-09-12  
Status: implemented baseline; controlled evaluation pending

## Pipeline overview

```text
microphone audio
    -> offline speech recognizer
    -> finalized caption turn
    -> speaker label assignment
    -> transcript store
    -> local gist generation
    -> optional user-reviewed reply draft
```

## Speaker handling

ShrutiSaathi supports two speaker modes.

### Predefined mode

The user configures participant names before starting. The selected label is attached to the next captured utterance. This is the safest baseline because it does not claim automatic identification.

### Automatic mode

Automatic mode is an experimental local clustering baseline:

1. The offline recognizer finalizes an utterance.
2. A local speaker embedding is extracted when sufficient voice evidence is available.
3. The app compares the embedding with existing clusters.
4. A cluster label is assigned in first-heard order.
5. Very short or weak segments are reported as insufficient voice evidence.

This is not biometric identity recognition. It is anonymous session-level clustering and must be described carefully in the paper.

## Gist generation

The conversation gist uses local generation when the Private AI Pack is installed. The model receives a bounded recent transcript and a strict task prompt:

- one factual gist,
- maximum 35 words,
- preserve topic and latest question/decision,
- preserve important entities,
- mention uncertainty when speakers are unsure,
- no copied dialogue,
- no invented facts,
- no speaker labels.

The full transcript remains available separately. The gist is a concise aid, not a complete memory of the session.

## Reply generation

Reply generation is based on:

- recent conversation context,
- current gist,
- user-intended words.

The system treats the typed text as intent. A generated reply is a private draft that the user must review before speaking or saving.

## Evaluation plan

| Area | Metric or observation | Required evidence |
|---|---|---|
| Speech recognition | WER/CER | Reference transcripts and app outputs. |
| Speaker clustering | DER/JER or label consistency | Multi-speaker scripted tests. |
| Gist quality | factuality and usefulness | Human-rated examples and failure cases. |
| Reply quality | relevance and safety | User-intent examples and human review. |
| Latency | p50/p95 cold and warm times | Real-phone timing logs. |
| Resource use | memory, battery, thermal | Real-phone monitoring. |
| Accessibility | readability and navigation | Font-scale, TalkBack, and touch-target tests. |

## Known limitations

- Overlapping speakers are not separated reliably.
- Very short speech segments may not produce useful speaker evidence.
- Speaker labels are session labels, not identities.
- Gist may omit details in long conversations.
- Local model output can still be wrong and must be reviewed.
- Background capture is not guaranteed in the current activity-based implementation.
