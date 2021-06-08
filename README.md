# Run server
`clojure -M:run-server`

# Upsert 
`curl -d '{"type": "upsert", "path":"/a/b/c", "value": 42}' -H 'Content-Type: application/json' http://localhost:8890/api`

# Remove
`curl -d '{"type": "remove", "path":"/a/b/c"}' -H 'Content-Type: application/json' http://localhost:8890/api`

# Run tests
`clojure -X:test`
