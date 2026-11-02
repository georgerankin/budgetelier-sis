# SIS API contract

## Request

`POST /v1/statements/parse`
`Content-Type: multipart/form-data`

| Part      | Type             | Required | Description                    |
|-----------|------------------|----------|--------------------------------|
| `file`    | `text/csv`       | yes      | One CSV export                 |
| `context` | `application/json` | yes    | The user's accounts, categories and keywords |

### `context` example

```json

{
    "bank_accounts": [
        {"id": 0, "account_name": "Main Account", "account_type": "checking", "iban": "DE00000000000000000001" },
        {"id": 1, "account_name": "Holiday Savings", "account_type": "savings", "iban": null }
    ],
    "categories": [
        {"id": 0, "type": "income", "name": "salary"},
        {"id": 4, "type": "expenses", "name": "rent"},
        {"id": 20, "type": "savings", "name": "emergency"}
    ],
    "keywords": [
        {"keyword": "employer", "category_id": 0},
        {"keyword": "landlord", "category_id": 4}
    ]
}
```

- `iban` is a string or `null`. If it is `null`, the SIS falls back to matching on `account_name`.
- `account_type` is a string or `null`.
- A transfer between two of the user's own accounts is always returned, flagged as internal with a reason.
- Out of a `savings` account: suggested type is `savings`. The `amount` is positive, as it appears on the statement.
- Any other internal transfer, including when `account_type` is `null`: no suggested type, and Budgetelier leaves it out of saving by default.
- The API caller is responsible for the amount's storage, regardless of whether its positive or negative

## Response

`200 OK` with `Content-Type: application/json`

```json
{
    "transactions": [
        {
            "source_row": 2,
            "booking_date": "2026-03-01",
            "value_date": "2026-03-01",
            "partner_name": "Example Landlord",
            "partner_iban": "DE00000000000000000099",
            "payment_reference": "March rent landlord",
            "amount": "-800.00",
            "suggested_category_id": 4,
            "category_name": "rent",
            "matched_keyword": "landlord",
            "suggested_type": "expenses",
            "is_internal_transfer": false,
            "internal_transfer_reason": null,
            "fingerprint": "placeholder-fingerprint-1"
        },
        {
            "source_row": 3,
            "booking_date": "2026-03-02",
            "value_date": "2026-03-02",
            "partner_name": "Holiday Savings",
            "partner_iban": null,
            "payment_reference": "Monthly transfer",
            "amount": "-100.00",
            "suggested_category_id": null,
            "category_name": "Uncategorised",
            "matched_keyword": null,
            "suggested_type": "savings",
            "is_internal_transfer": true,
            "internal_transfer_reason": "Partner name matches account: Holiday Savings",
            "fingerprint": "placeholder-fingerprint-2"
        }
    ],
    "unparsed": [
        { 
            "source_row": 4, 
            "reason": "Invalid amount" 
        }
    ]
}
```

- `source_row` is the line number in the CSV, where the header is row 1.
- `amount` is a decimal string, signed exactly as it appears on the bank statement. Budgetelier is responsible for converting it for storage.

## Errors

If the whole request can't be processed, the SIS returns an error instead of a partial result. The message never includes CSV contents.

| Situation | Status |
|-----------|--------|
| The `file` or `context` part is missing | `400 Bad Request` |
| `context` isn't valid JSON or doesn't match the expected shape | `400 Bad Request` |
| `file` isn't a readable CSV, or is missing a required column | `400 Bad Request` |

```json
{ "error": "The CSV is missing required columns: Booking Date, Amount (EUR)" }
```

API Caller decides how to show the error to the user.



