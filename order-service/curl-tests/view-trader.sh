echo "obtaining token..."
TOKEN_TRADER=$(curl -sS -X POST \
          "https://auth.3-111-205-87.sslip.io/realms/capstone/protocol/openid-connect/token" \
            -d "grant_type=password" \
              -d "client_id=capstone-frontend" \
                --data-urlencode "username=team-1-trader1" \
                  --data-urlencode "password=1ocK9eLopHJw" \
                    | jq -r '.access_token')

# echo "obtained token!, token is"
# echo $TOKEN_TRADER
echo "testing trader token on view"

curl -sS http://localhost:8080/api/orders/view \
          -H "Authorization: Bearer $TOKEN_TRADER" \
            -H "Accept: application/json" | jq


echo "testing trader token on view all (expected 401)"

curl -i http://localhost:8080/api/orders/view-all \
          -H "Authorization: Bearer $TOKEN_TRADER"



echo "testing trader token submiting trade (valid)"

curl -i -X POST http://localhost:8080/api/orders/submit \
          -H "Authorization: Bearer $TOKEN_TRADER" \
            -H "Content-Type: application/json" \
              -d '{
    "instrumentId": "AAPL",
        "side": "BUY",
            "quantity": 10,
                "limitPrice": 185.50
                  }'


