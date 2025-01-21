#!/usr/bin/env python3

import requests
import json

# The remote URL providing the array of orders (with orderStatus, orderValidationCode, etc.)
ORDERS_URL = "https://ilp-rest-2024.azurewebsites.net/orders"

# The endpoint that we actually send each order to:
ENDPOINT_URL = "http://localhost:8080/validateOrder"

# The output filename for the generated Postman collection in the same directory.
OUTPUT_FILENAME = "systemTestCollection.json"

def build_test_script(expected_status, expected_code):
    """
    Build a JavaScript test script for Postman verifying:
      - HTTP 200 status
      - jsonData.orderStatus == expected_status
      - jsonData.orderValidationCode == expected_code
    """
    lines = [
        'pm.test("Status code is 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        f'pm.test("Order status is {expected_status}", () => {{',
        '    const jsonData = pm.response.json();',
        f'    pm.expect(jsonData.orderStatus).to.eql("{expected_status}");',
        '});',
        '',
        f'pm.test("Order validation Code is {expected_code}", () => {{',
        '    const jsonData = pm.response.json();',
        f'    pm.expect(jsonData.orderValidationCode).to.eql("{expected_code}");',
        '});'
    ]
    # Join them with newlines so it's a valid Postman test script
    return '\n'.join(lines)

def main():
    # 1) Fetch the array of orders from the remote URL
    print(f"Fetching orders from {ORDERS_URL} ...")
    response = requests.get(ORDERS_URL)
    response.raise_for_status()  # raise exception if HTTP error
    orders = response.json()     # parse JSON -> Python list

    # 2) Build a Postman collection "items" array
    items = []

    for order in orders:
        # The order has "orderStatus" and "orderValidationCode" which are the "expected" results
        expected_status = order["orderStatus"]
        expected_code = order["orderValidationCode"]

        # Copy the order, then remove those fields from the actual body
        order_copy = dict(order)
        order_copy.pop("orderStatus", None)
        order_copy.pop("orderValidationCode", None)

        # Convert to JSON
        body_string = json.dumps(order_copy, indent=2)

        # Build the test script
        script_text = build_test_script(expected_status, expected_code)

        # Create a Postman item
        item = {
            "name": f'Order #{order["orderNo"]} - Expect {expected_status}/{expected_code}',
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": script_text.split('\n')
                    }
                }
            ],
            "request": {
                "method": "POST",
                "header": [],
                "body": {
                    "mode": "raw",
                    "raw": body_string,
                    "options": {
                        "raw": {
                            "language": "json"
                        }
                    }
                },
                "url": {
                    "raw": ENDPOINT_URL,
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8080",
                    "path": ["validateOrder"]
                }
            }
        }

        items.append(item)

    # 3) Build the entire Postman collection object
    collection = {
        "info": {
            "_postman_id": "from-remote-orders",
            "name": "systemTest - fromRemoteArray",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "item": items
    }

    # 4) Write the JSON to a file in the same directory
    with open(OUTPUT_FILENAME, "w", encoding="utf-8") as f:
        json.dump(collection, f, indent=2)

    print(f"Generated Postman collection saved to: {OUTPUT_FILENAME}")

if __name__ == "__main__":
    main()
