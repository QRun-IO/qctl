# qctl

Command-line interface for QQQ applications.

**For:** Developers and operators managing QQQ applications from the terminal  
**Status:** Early Development

## Why This Exists

QQQ applications expose REST APIs for all operations. For automation, scripting, and quick tasks, a CLI is faster than navigating the dashboard. qctl provides terminal access to QQQ functionality.

## Features

- **Record Operations** - Query, create, update, delete records
- **Process Execution** - Run ETL processes from the command line
- **Configuration** - Manage application settings
- **Multiple Environments** - Switch between dev, staging, production

## Quick Start

### Installation

```bash
# Download binary
curl -LO https://github.com/QRun-IO/qctl/releases/latest/download/qctl
chmod +x qctl
sudo mv qctl /usr/local/bin/

# Or with Go
go install github.com/QRun-IO/qctl@latest
```

### Configuration

```bash
# Set up connection
qctl config set-context dev --server http://localhost:8000
qctl config use-context dev
```

### Basic Usage

```bash
# List records
qctl get orders --limit 10

# Create record
qctl create order --data '{"customerName": "Acme Corp", "total": 100}'

# Run process
qctl run process orderSync
```

## Usage

### Querying Records

```bash
# Get all customers
qctl get customers

# Filter with criteria
qctl get orders --filter "status=pending"

# Output as JSON
qctl get orders -o json

# Get single record
qctl get order 123
```

### Managing Records

```bash
# Create
qctl create customer --data '{"name": "New Customer"}'

# Update
qctl update order 123 --set status=shipped

# Delete
qctl delete order 123
```

### Running Processes

```bash
# Run process
qctl run process dailySync

# Run with parameters
qctl run process import --param file=/data/input.csv

# Check process status
qctl get process-runs --filter "processName=dailySync"
```

## Configuration

Config file location: `~/.qctl/config.yaml`

```yaml
contexts:
  dev:
    server: http://localhost:8000
    token: dev-token
  prod:
    server: https://app.example.com
    token: ${QQQ_PROD_TOKEN}
current-context: dev
```

## Project Status

Early development. Core functionality is being implemented.

### Roadmap

- Authentication support (OAuth, API keys)
- Interactive mode
- Bash/Zsh completion
- Watch mode for records

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

Proprietary - QRun.IO
