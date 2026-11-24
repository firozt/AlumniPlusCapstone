#!/bin/bash
set -euo pipefail

echo "obtaining token..."
TOKEN_TRADER=$(curl -fsS -X POST \
          "https://auth.3-111-205-87.sslip.io/realms/capstone/protocol/openid-connect/token" \
            -d "grant_type=password" \
              -d "client_id=capstone-frontend" \
                --data-urlencode "username=team-1-trader1" \
                  --data-urlencode "password=1ocK9eLopHJw" \
                    | jq -er '.access_token // empty')

echo "obtained token!"

echo "======================"
echo "submiting with instrument microsoft"

ORDER_ID=$(curl -fsS -X POST http://localhost:8080/api/orders/submit \
  -H "Authorization: Bearer $TOKEN_TRADER" \
  -H "Content-Type: application/json" \
  -d '{
    "instrumentId": "MSFT",
    "side": "BUY",
    "quantity": 10,
    "limitPrice": 185.50
  }' | jq -er '.orderId')

echo "order id - $ORDER_ID"

echo "checking view for current state"

curl -fsS http://localhost:8080/api/orders/view \
    -H "Authorization: Bearer $TOKEN_TRADER" \
    -H "Content-Type: application/json" | jq


echo "cancelling order"

curl -fsS -X POST "http://localhost:8080/api/orders/cancel/$ORDER_ID" \
    -H "Authorization: Bearer $TOKEN_TRADER" \
    -H "Content-Type: application/json" \
    | jq -e --argjson id "$ORDER_ID" \
        'select(.orderId == $id and .status == "CANCELLED")'


echo "checking order again should be cancelled"

# Allow time for the default ten-second acceptance task to finish.
sleep "${CANCEL_CHECK_WAIT_SECONDS:-15}"

curl -fsS http://localhost:8080/api/orders/view \
    -H "Authorization: Bearer $TOKEN_TRADER" \
    -H "Content-Type: application/json" | \
    jq -e --argjson id "$ORDER_ID" \
        'any(.[]; .orderId == $id and .status == "CANCELLED")'

echo "Order $ORDER_ID is still CANCELLED."

