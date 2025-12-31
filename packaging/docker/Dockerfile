FROM alpine:3.19 AS base

ARG TARGETARCH

RUN apk add --no-cache ca-certificates git

COPY qctl-linux-${TARGETARCH} /usr/local/bin/qctl
RUN chmod +x /usr/local/bin/qctl

ENTRYPOINT ["qctl"]
CMD ["--help"]
