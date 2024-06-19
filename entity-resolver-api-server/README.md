# Telicent Search API

This subdirectory provides the implementation of the Search API as formally defined by the
[`entity-resolution-api.yaml`](../entity-resolution-api.yaml) file in the root of this repository.

This API provides a single operations:

- `PUT /similarity/` for making E.R. queries

# Requirements

See parent [README.md](../README.md) for Requirements

# Build

See parent [README.md](../README.md) for Build instructions.

# Run

See parent [README.md](../README.md) for Run instructions.

# Usage

Once running you can make requests in a separate terminal window, or using the HTTP/REST client of your choice e.g.

```bash
$ curl 'http://localhost:8081/documents?query=foo'
```

Note that all responses will be in JSON format, so you may find it helpful to pipe them into `jq` to make them more
readable.

Please refer to [`entity-resolution-api.yaml`](../entity-resolution-api.yaml) for the full set of acceptable parameters and the results
formats.

