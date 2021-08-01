# CastService
See problem description for CastService functional spec. 
This service implementation uses RMI for service calls and TCP socket for streaming of serialized [Cast](https://github.com/dkhokhlov/CastService/blob/main/src/com/trumid/castservice/Cast.java) objects.

## Interface definition

Interface is defined in: 

[com.trumid.castservice.ICastService](https://github.com/dkhokhlov/CastService/blob/main/src/com/trumid/castservice/ICastService.java#L8)

## Run standalone service

```
$ java com.trumid.castservice.Main
CastService v1.0.0
Ready

$ ps aux | grep com.trumid.castservice.Main
owner    10612 12.0  0.2 7213088 33752 pts/4   Sl+  19:12   0:00 java com.trumid.castservice.Main
```

## Terminate standalone service

1) Send TERM signal

```
$ kill -term 10612
```

2) Send Ctrl-C to running app

## Debug

1) Logging goes to stdout 

2) Print stack trace to stderr:

```
$ kill -quit 10612
```

## Life cycle
- needs to be restarted at EOD to clean accumulated books
