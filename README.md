# tOSCE CLI

> **⚠️ BETA SOFTWARE — USE AT YOUR OWN RISK**
>
> This CLI is in early beta. It may contain bugs, produce unexpected results, or cause data loss when interacting with your tOSCE server. It has not been tested against all server versions or configurations.
> **Do not use in production exam environments without thorough testing.** No warranties, express or implied, are provided. Use entirely at your own risk.

A command-line interface for the **tOSCE server** — a platform for conducting OSCE (Objective Structured Clinical Examination) sessions. Designed for automation and AI agent use: all output is JSON.

## Installation

Native binaries (no JVM required) — permanent direct-download links, always pointing to the latest release:

| Platform | Permanent URL |
|----------|---------------|
| Linux (x86_64) | https://github.com/MeMyselfI/tosce-cli/releases/latest/download/tosce-linux-amd64 |
| macOS (Apple Silicon) | https://github.com/MeMyselfI/tosce-cli/releases/latest/download/tosce-macos-arm64 |
| Windows (x86_64) | https://github.com/MeMyselfI/tosce-cli/releases/latest/download/tosce-windows-amd64.exe |

All releases: https://github.com/MeMyselfI/tosce-cli/releases

```bash
# Linux / macOS
curl -fsSL https://github.com/MeMyselfI/tosce-cli/releases/latest/download/tosce-linux-amd64 -o tosce
chmod +x tosce
mv tosce /usr/local/bin/tosce

# Windows (PowerShell)
Invoke-WebRequest https://github.com/MeMyselfI/tosce-cli/releases/latest/download/tosce-windows-amd64.exe -OutFile tosce.exe
```

## Quick Start

```bash
# Save credentials (writes to ~/.tosce/config)
tosce login --url https://127.0.0.1:8443 --user admin --pass secret --insecure

# List all OSCEs — JSON (default, ideal for AI agents)
tosce osce list

# List all OSCEs — ad-hoc without saved credentials
tosce --url https://127.0.0.1:8443 --user admin --pass secret --insecure osce list

# List all OSCEs — human-readable table
tosce --url https://127.0.0.1:8443 --user admin --pass secret --insecure --table osce list

# Pretty-print JSON output
tosce osce list --pretty

# Get a specific OSCE
tosce osce get 42
```

Example table output:

```
| ID   | NAME                        | DATE              | STATUS   |
+------+-----------------------------+-------------------+----------+
| 1    | OSCE Sommersemester 2025    | 20250601090000000 | active   |
| 2    | OSCE Wintersemester 2025/26 | 20251201090000000 | planned  |
```

## Authentication

Credentials are resolved in this priority order (highest first):

1. **CLI flags**: `--url`, `--user`, `--pass`
2. **Environment variables**: `TOSCE_URL`, `TOSCE_USER`, `TOSCE_PASSWORD`, `TOSCE_INSECURE`
3. **Config file**: `~/.tosce/config`

```bash
# Using environment variables (ideal for CI/AI agents)
export TOSCE_URL=https://127.0.0.1:8443
export TOSCE_USER=admin
export TOSCE_PASSWORD=secret
tosce osce list
```

## Commands

### OSCE Management

```bash
tosce osce list [--current]                         # List OSCEs
tosce osce get <id>                                  # Get OSCE details
tosce osce create --file exam.xml                    # Import OSCE from XML/ZIP
tosce osce delete <id>                               # Delete OSCE
tosce osce export <id> --format xml|html|pdf         # Export OSCE
tosce osce result <id> --format xlsx|pdf|ims-xml     # Download results
tosce osce phrases list <id>                         # List phrase tree
tosce osce phrases import <id> --file phrases.xlsx   # Import phrases
```

### Examinee Management

```bash
tosce examinee list [--osce-id <id>]
tosce examinee create --osce-id <id> --data '{"name":"Doe","firstname":"Jane"}'
tosce examinee import --osce-id <id> --file examinees.xls
tosce examinee delete <id>
```

### Examiner Management

```bash
tosce examiner list
tosce examiner create --data '{"name":"Smith","firstname":"John"}'
tosce examiner update <id> --data '{...}'
tosce examiner delete <id>
```

### Actor Management

```bash
tosce actor create --data '{"name":"Patient A"}'
tosce actor update <id> --data '{...}'
tosce actor delete <id>
```

### QR Codes

```bash
tosce qr login <osce-id> --out login-qr.pdf
tosce qr examinees <osce-id> [--sorted] --out examinees-qr.zip
```

### Backup & Restore

```bash
tosce backup dump <osce-id> --out backup.zip
tosce backup restore --file backup.zip
```

### Backend Users

```bash
tosce user list
tosce user create <id> --data '{"login":"jdoe","password":"secret"}'
tosce user update <id> --data '{...}'
tosce user delete <id>
```

### Other

```bash
tosce client list        # List connected devices
tosce conf get           # Get server configuration
tosce server info        # Server version and info
tosce server letsencrypt # Refresh Let's Encrypt certificate
```

## Global Options

| Option | Description |
|--------|-------------|
| `--url URL` | Server URL (overrides config/env) |
| `--user, -u USER` | Username |
| `--pass, -p PASS` | Password |
| `--insecure, -k` | Skip TLS certificate verification |
| `--pretty` | Pretty-print JSON output |
| `--table` | Render result as human-readable table |
| `--help` | Show help |
| `--version` | Show version |

## Building from Source

Requires Java 21 and Maven 3.9+.

```bash
# Fat JAR (requires JVM)
mvn package
java -jar target/tosce-cli-1.0.0.jar osce list

# Native binary (requires GraalVM 21)
mvn -Pnative package
./target/tosce osce list
```

## For AI Agents

All output is newline-delimited JSON. Use `--pretty` for human-readable output. Exit codes follow standard Unix conventions: `0` = success, `1` = error.

```bash
# Pipe to jq for filtering
tosce osce list | jq '.root[] | {id: .id, name: .name}'

# Check success programmatically
result=$(tosce osce get 42)
success=$(echo "$result" | jq -r '.success')
```

## License

MIT
