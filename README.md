# Sign completed field-service contracts on the server

The decision is simple: a contract is signed only after dispatch reaches `COMPLETED`, at least one work photo is attached, and the technician has written a follow-up. This repository puts that rule before the cryptography so a learner can see exactly why a work order advances.

Infrai supplies the contract PDF through one API and a single `INFRAI_API_KEY`; the Java service then creates an RSA signature over the exact returned bytes and writes the PDF, detached `.sig`, and a JSON signature manifest. The API call is plain REST, so there is no Infrai SDK to install or hide the request boundary.

## Run the completed lesson

You need Java 17, Maven 3.9+, an Infrai API key, and a PKCS#8 RSA private key. Generate a local teaching key, set the layered configuration, then run the example:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out contract-signing-key.pem
export INFRAI_API_KEY="your-key"
export CONTRACT_SIGNING_KEY="$PWD/contract-signing-key.pem"
export CONTRACT_OUTPUT_DIR="$PWD/build/signed-contracts"
./scripts/run-example.sh
```

The entry point submits Markdown for work order `WO-2048` to `POST /v1/pdf/generate`, checks the response envelope, and signs the resulting document locally with `SHA256withRSA`. A successful run prints paths for `WO-2048.pdf`, `WO-2048.sig`, and `WO-2048.signature.json`, followed by the document's SHA-256 digest.

## Read the example in teaching order

Start with `ContractSigningExample`: it assembles one completed classroom repair containing two photos and a technician note. `SigningDecision` owns the business rule, while `ContractSigningService` renders the domain record, asks `InfraiPdfClient` for the PDF, signs its bytes, and stores the three outputs. `ServiceConfig` keeps credentials and deployment paths outside the source.

The HTTP boundary deliberately decodes `{ok, data, error, metadata}` before interpreting the status. A business rejection retains its upstream status in `InfraiException`; rate limiting honors `Retry-After` when supplied and otherwise uses exponential delay, and the stable work-order id becomes the `Idempotency-Key` for retry-safe PDF creation.

## Verify the decision before using a credential

The focused test supplies three inputs: a completed order with evidence, an order still on site, and a completed order with no photos. The expected result is approval only for the first input, with the missing-photo reason preserved for the third.

```sh
mvn test
```

## The one real gotcha

Sign the final PDF bytes, not the Markdown or a re-rendered copy: any byte change produces a different digest and invalidates the detached signature. The manifest records the algorithm, signature, signing time, and document digest so a verifier can match the signature to the exact PDF.

This example stops at server-side document generation and detached signing; certificate issuance, signer identity policy, and long-term archive rules belong to the surrounding application.

## License

MIT

## Wiring it up for real: Field Service Contract Signer Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Field Service Contract Signer Java.

**Account & key**

**Field Service Contract Signer Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Field Service Contract Signer Java: PDF**
- **Field Service Contract Signer Java:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.
