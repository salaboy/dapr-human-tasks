# dapr-human-tasks
Simple PoC for Dapr Human Task Service proposal




## Test locally from source

```
go run main.go
```

```
http POST :8080/tasks/ < task.json
```

## Generating APIs from Spec
APIs generated from OpenAPI spec (kodata/docs/openapi.yaml)

```
oapi-codegen -generate chi-server -package main kodata/docs/openapi.yaml > api/api.go
```