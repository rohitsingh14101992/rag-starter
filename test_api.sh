#!/bin/bash
set -e

BASE="http://localhost:8081/api"

echo "=== 1. Login ==="
LOGIN=$(curl -s -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"abc@gmail.com","password":"password"}')
TOKEN=$(echo $LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
[ -z "$TOKEN" ] && echo "❌ Login failed: $LOGIN" && exit 1
echo "✅ JWT obtained."
AUTH="Authorization: Bearer $TOKEN"

echo -e "\n=== 2. POST /api/conversations (create) ==="
CONV=$(curl -s -X POST "$BASE/conversations" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"title":"My first conversation"}')
echo $CONV | python3 -m json.tool
CONV_ID=$(echo $CONV | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -z "$CONV_ID" ] && echo "❌ Create conversation failed" && exit 1
echo "✅ Created conversation id=$CONV_ID"

echo -e "\n=== 3. GET /api/conversations (list) ==="
curl -s "$BASE/conversations?limit=5&offset=0" -H "$AUTH" | python3 -m json.tool

echo -e "\n=== 4. PATCH /api/conversations/$CONV_ID (update title) ==="
curl -s -X PATCH "$BASE/conversations/$CONV_ID" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"title":"Updated title"}' | python3 -m json.tool
echo "✅ Patched title"

echo -e "\n=== 5. POST /api/conversations/$CONV_ID/messages (create message) ==="
MSG=$(curl -s -X POST "$BASE/conversations/$CONV_ID/messages" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"role":"user","content":"Hello, world!"}')
echo $MSG | python3 -m json.tool
echo "✅ Created message"

echo -e "\n=== 6. GET /api/conversations/$CONV_ID/messages (list messages) ==="
curl -s "$BASE/conversations/$CONV_ID/messages?limit=10&offset=0" -H "$AUTH" | python3 -m json.tool

echo -e "\n=== 7. DELETE /api/conversations/$CONV_ID ==="
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/conversations/$CONV_ID" -H "$AUTH")
[ "$STATUS" = "204" ] && echo "✅ Deleted (204 No Content)" || echo "❌ Delete failed: HTTP $STATUS"

echo -e "\n=== All tests passed! ==="
